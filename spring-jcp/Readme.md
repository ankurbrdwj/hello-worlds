Banner
JetBrains Logo
Back to jobs
Senior Software Developer (JCP Core Foundation Licensing and Quotation)
Belgrade, Serbia; Berlin, Germany; Limassol, Cyprus; Madrid, Spain; Munich, Germany; Paphos, Cyprus; Prague, Czech Republic; Remote, Germany; Warsaw, Poland; Yerevan, Armenia
If you want to build and own the systems that decide what JetBrains customers are entitled to use and bill them accurately for it, this is for you. You will focus on designing backend services, keeping complex commercial flows correct, and tackling hard distributed systems challenges on the critical path of every paid request.

About JetBrains

We create intelligent software development tools for developers and teams. More than 15 million users, over 300,000 companies, and 88 of the Fortune Global Top 100 rely on our products to solve real, complex problems. Our mission is simple: make development teams more productive and AI adoptable at scale.

What you’ll do

At JetBrains, the Cloud Platform department builds the underlying infrastructure that powers authentication, paid capabilities, usage metering, and accounting. Within this department, the Licensing & Billing team sits right at the center of our commercial model, turning what a customer buys into what they are actually allowed to use, and accounting for that consumption accurately enough to bill on.

The complexity of our entitlement, quota, and metering systems continues to grow. In this role, you will own the correctness and evolution of these backend services, take full ownership of end-to-end data flows, tackle distributed consistency across unreliable upstream systems, and ensure revenue-affecting data migrations happen cleanly without taking services offline.

Day to day, you will:

    Design, build, and operate backend services that decide what a customer is entitled to and what their usage costs.
    Own flows end to end, from ingesting licensing and purchase data from upstream systems to enforcing quotas and recording consumption.
    Evolve our domain model to handle new commercial structures, combining seat-based plans and usage-based models.
    Design integrations with external systems, ensuring correctness through idempotency, ordering, reconciliation, and eventual consistency.
    Plan and execute live data migrations using feature flags, dual writes, backfills, and verification at every stage.
    Build observability into our systems by defining invariants, metrics, and alerts to catch silent divergence before customers do.
    Design internal APIs with explicit focus on versioning, backward compatibility, and developer experience for other JetBrains teams.
    Improve system performance and reliability under continuous production load while decommissioning legacy subsystems.
    Write integration tests against real dependencies and maintain reliable test infrastructure.
    Investigate production incidents, find root causes, and follow through on lasting fixes.
    What you’ll bring
    
    An engineer’s dedication to accuracy, especially in domains where system errors have direct financial impact.
    Pragmatic judgment when designing distributed systems where correctness under unreliable delivery is the main constraint.
    A habit of trusting metrics, traces, and logs to understand production behavior rather than relying on assumptions.
    Comfort navigating and simplifying large existing codebases containing legacy decisions.
    Clear, direct communication skills to agree on API contracts across teams with different priorities.
    A focus on testing as an essential design tool rather than an afterthought.
    What you’ll need
    
    A proven track record of building and operating backend services on the JVM platform using Kotlin or Java.
    A solid working knowledge of relational databases, including transactions, isolation levels, locking, indexing, and live schema migrations (PostgreSQL preferred).
    A deep understanding of distributed systems patterns, such as idempotency, retry strategies, eventual consistency, and data reconciliation.
    Practical experience making live data migrations or system changes without downtime using feature flags and staged rollouts.
    Experience designing versioned, backward-compatible APIs for other engineering teams.
    Hands-on experience with event-driven architectures and message brokers like Kafka.
    The ability to diagnose, debug, and resolve production incidents in complex environments.
    Proficiency in both written and spoken English.
    What success looks like
    
    Success in this role means building a bulletproof entitlement and metering foundation that product teams can trust without hesitation. You will have successfully migrated live commercial systems to support new billing models without downtime or data divergence. Silent errors will be caught automatically by system invariants before affecting users, and internal teams will integrate seamlessly with your clean, versioned APIs.


1. Idempotency: The same usage event sent twice, with the same key, is recorded once. Concurrent duplicates must not double-count.
2. Out-of-order and late events: Totals come out the same whatever order events arrive in.
3. Quota enforcement under concurrency: N parallel requests against a quota of K never allow more than K. This needs a real Postgres, because mocks hide it.
4. Transactional consumption: If recording usage fails halfway, neither the usage record nor the quota counter changes.
5. Reconciliation: Deliberately corrupt a counter, run the reconciler, and check that it detects the drift and repairs it.
6. Live migration: During a dual write, both stores agree. After a backfill, a verification pass shows zero mismatches. Flipping the feature flag back and forth loses nothing.
7. Seat-based plus usage-based plans: Combined plans give correct entitlements, including at the boundary where a quota is exactly used up (<= vs <).
8. Poison and upstream failures: A malformed event goes to a dead-letter path without blocking the stream. Retries with backoff don't duplicate.
9. API compatibility: An old client against a new version still works. A contract test pins the response shape.
10. Invariants/metrics: A test that asserts something like "sum of events equals the counter" and fires an alert when it breaks.  