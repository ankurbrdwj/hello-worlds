package com.ankur.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sliding Window Log Rate Limiter — records the exact timestamp of every allowed request.
 *
 * Algorithm: keep a per-key deque of request timestamps. On each call, evict timestamps
 * older than the window, then allow if the remaining count is below maxEvents.
 *
 * How it differs from the other three implementations:
 *
 *  vs CounterLimiterImpl (Sliding Window Counter):
 *    - CounterLimiter approximates the window by blending two fixed buckets using a weighted
 *      formula ((prevCount * overlap%) + currentCount). It trades perfect accuracy for O(1)
 *      memory per key regardless of traffic.
 *    - LogLimiter is exact: every timestamp is stored, so a burst of 1000 req/s means 1000
 *      Long entries in the deque. Memory scales with request volume, not just key count.
 *
 *  vs TokenRateLimiter (Token Bucket):
 *    - TokenRateLimiter allows controlled bursts: if tokens have accumulated, a client can
 *      fire N requests instantly (up to bucket capacity). Requests are not tied to a wall-clock
 *      window at all — only to the refill schedule.
 *    - LogLimiter enforces a hard count within a rolling window with no burst allowance beyond
 *      maxEvents; the oldest event must have aged out before a new one is accepted.
 *
 *  vs LeakyBucketRateLimiter:
 *    - LeakyBucket enforces a fixed *output* rate: requests are queued and "leak" at
 *      leakRateMillis intervals, making the downstream rate perfectly smooth but adding latency.
 *    - LogLimiter is count-based with no queuing: requests are either accepted immediately or
 *      rejected, and the effective rate can vary within the window as long as the total stays
 *      under maxEvents.
 *
 * Trade-offs:
 *  ✅ Pixel-perfect accuracy — no interpolation artefacts at window boundaries
 *  ✅ True rolling window (not two aligned fixed buckets)
 *  ❌ Memory cost proportional to traffic volume (each allowed request stores a Long)
 *  ❌ Under high concurrency, pollFirst/addLast on separate threads can still produce a small
 *     race window; add external synchronization per-key if strict atomicity is required
 */
public class LogLimiterImpl implements RateLimiter {

    private final int maxEvents;
    private final long windowMillis;
    private final Map<String, ConcurrentLinkedDeque<Long>> eventsPerKey;

    public LogLimiterImpl(int maxEvents, long windowMillis) {
        this.maxEvents = maxEvents;
        this.windowMillis = windowMillis;
        this.eventsPerKey= new ConcurrentHashMap<>();
    }

    /**
     * Sample trace with windowMillis = 10_000 (10s window) and maxEvents = 2, all calls
     * for the same key, showing the deque contents evolving across calls:
     *
     * 1) allow(key, 12_000)
     *    windowStartTime = 12_000 - 10_000 = 2_000
     *    windowEndTime   = 12_000
     *    events before = []               -> nothing to evict
     *    events.size()=0 < maxEvents(2)   -> ALLOW, events after = [12_000]
     *
     * 2) allow(key, 15_000)
     *    windowStartTime = 15_000 - 10_000 = 5_000
     *    windowEndTime   = 15_000
     *    events before = [12_000]         -> peekFirst=12_000, 12_000 <= 5_000? no -> nothing evicted
     *    events.size()=1 < maxEvents(2)   -> ALLOW, events after = [12_000, 15_000]
     *
     * 3) allow(key, 22_000)
     *    windowStartTime = 22_000 - 10_000 = 12_000
     *    windowEndTime   = 22_000
     *    events before = [12_000, 15_000] -> peekFirst=12_000, 12_000 <= 12_000? yes -> evicted (boundary inclusive)
     *                                      -> peekFirst=15_000, 15_000 <= 12_000? no  -> stop evicting
     *    events.size()=1 < maxEvents(2)   -> ALLOW, events after = [15_000, 22_000]
     *
     * Contrast: if call 3 had instead been allow(key, 16_000) (no eviction, since
     * windowStartTime=6_000 and 12_000 > 6_000), events.size()=2 is NOT < maxEvents(2)
     * -> REJECT, events unchanged = [12_000, 15_000].
     */
    @Override
    public boolean allow(String key, long nowMillis) {
        long windowStartTime = nowMillis-this.windowMillis;

        // getEvents for this key
        ConcurrentLinkedDeque<Long> events = this.eventsPerKey.computeIfAbsent(key,k-> new ConcurrentLinkedDeque<>());
        // remove old events
        // Queue is first-in-first-out we always remove from head by poll or remove
        // Any event timestamp that is windowMillis milliseconds (or more)(window time) older than nowMillis
        // (i.e. <= windowStartTime) has fallen outside the rolling window and is deleted from the list
        Long first;
        while((first = events.peekFirst()) != null && first <= windowStartTime){
            events.pollFirst();
        }
        // check if under maxEvents
        if(events.size()<this.maxEvents){
            // we always add last in queue
            events.addLast(nowMillis);
            return true;
        }
        return false;
    }
}
