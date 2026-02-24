package com.ankur.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LogLimiterImpl implements RateLimiter {

    private final int maxEvents;
    private final long windowMillis;
    private final Map<String, ConcurrentLinkedDeque<Long>> eventsPerKey;

    public LogLimiterImpl(int maxEvents, long windowMillis) {
        this.maxEvents = maxEvents;
        this.windowMillis = windowMillis;
        this.eventsPerKey= new ConcurrentHashMap<>();
    }

    @Override
    public boolean allow(String key, long nowMillis) {
        long windowSartTime = nowMillis-this.windowMillis;
        long windowEndTime = nowMillis;

        // getEvents for this key
        ConcurrentLinkedDeque<Long> events = this.eventsPerKey.computeIfAbsent(key,k-> new ConcurrentLinkedDeque<>());
        // remove old events
        // Queue is first-in-first-out we always remove from head by poll or remove
        Long first;
        while((first = events.peekFirst()) != null && first <= windowSartTime){
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
