# Technical Decision Framework — Cost/Benefit Justification Methodology

A reusable process for justifying non-trivial technical decisions (architecture changes,
new infrastructure, framework adoption, build-vs-buy, "should we shard/queue/cache/rewrite
this") at department or org scale. The goal is to replace "senior engineer's gut feeling"
or "whoever argued loudest" with a documented, comparable, revisitable decision trail.

This framework is deliberately generic — `spring-postgres/sharding.md` is a worked example
of applying it to one specific decision (whether to shard a Postgres database). Use this
document as the template; use that one as a reference for what "done" looks like.

---

## 1. When this process applies

Not every decision needs this. Use it when **any** of these are true:

- The change is expensive or risky to reverse (data migration, new infra, a new
  dependency the org will run for years, a schema/API change external consumers depend on)
- The blast radius extends beyond one team (shared infra, a pattern other teams will copy,
  a cost line the finance/platform org will see)
- Reasonable engineers disagree about whether to do it
- The decision recurs (this is the third team asking "should we do X" — worth a durable
  answer instead of re-litigating from scratch each time)

Skip it for reversible, team-local, low-cost choices — a library swap inside one service,
a refactor with no external contract change. Applying this process to every decision is
its own failure mode: it becomes theater, and people route around it.

---

## 2. The five-part justification

Every justification produced under this framework has five parts, in this order. A
decision isn't ready for review until all five are filled in — not because the process
demands paperwork, but because skipping any one of them is exactly how bad decisions get
made (see Section 7, Anti-patterns).

### 2.1 Problem statement
What's actually broken or missing, in terms of observed symptoms, not proposed solutions.

- Bad: "We need to add caching."
- Good: "p99 latency on `/orders` is 1.8s, SLA is 500ms. `pg_stat_statements` shows the
  same read-heavy query pattern accounts for 70% of DB time. Confirmed via load test that
  this recurs under normal, not just peak, traffic."

If you can't state the problem without naming the solution, you don't have a problem
statement yet — go measure something.

### 2.2 Options considered (minimum three)
Including "do nothing" and "the boring option." A justification that only presents one
option isn't a cost-benefit analysis, it's a pitch. For each option, capture:

- What it is, one paragraph
- Rough cost (Section 3) and benefit (Section 4)
- Why it was or wasn't carried forward to the recommendation

The "do nothing / defer" option must always be listed explicitly, with its cost (the
problem keeps happening, or gets worse on some timeline) — this is what the other options
are being compared against, and it's the option most often skipped because it's
unglamorous.

### 2.3 Cost model
See Section 3. Quantify what you can, name what you can't quantify rather than omitting it.

### 2.4 Benefit model
See Section 4. Same rule: quantify where possible, name explicitly where not.

### 2.5 Recommendation + decision record
The chosen option, the reasoning tying cost to benefit, who approved it, and — critically —
**what would change this decision**. See Section 6 for the record format.

---

## 3. Cost taxonomy

Technical costs are chronically underestimated because only the first row below is
visible when the decision is made. Walk all six every time.

| Cost category | What it includes | Common underestimation trap |
|---|---|---|
| **Build cost** | Engineering time to design, implement, test, and ship | The estimate people actually make — usually the smallest true cost |
| **Run cost** | Infra spend, on-call load, monitoring/alerting to build and maintain, incident response | "We'll figure out ops later" — later is expensive and happens during an incident |
| **Migration cost** | Data migration, dual-write/dual-read periods, backfills, rollback plan | Migration is frequently 2-3x the build cost of the new system itself |
| **Opportunity cost** | What the team isn't doing instead, for how long | Rarely stated explicitly; always real |
| **Cognitive/onboarding cost** | Every new hire and every engineer touching this system now has more to learn; documentation debt | Compounds silently — invisible until ramp-up time is measured |
| **Exit cost** | What it costs to reverse this decision in 2 years if it's wrong | If nobody can answer this, that itself is a cost — it means the decision is a one-way door being treated as reversible |

For any decision above the threshold in Section 1, all six rows should have at least a
sentence, even if it's "negligible, because X." Silent omission is how a real cost gets
discovered in production instead of in the proposal.

---

## 4. Benefit taxonomy

Benefits get inflated the mirror-image way costs get deflated — vague, aspirational, and
hard to falsify later. Force every claimed benefit into one of these buckets with a
number or an explicit named assumption attached:

| Benefit category | What "good" looks like | What to reject |
|---|---|---|
| **Performance** | "p99 drops from 1.8s to est. 300ms, based on X benchmark/prior migration" | "Should be faster" with no baseline or estimate |
| **Cost reduction** | "$X/month infra saved, based on current usage × unit price" | "More efficient" with no dollar figure |
| **Revenue / product enablement** | "Unblocks feature X, which sales has N deals waiting on" | "Strategic" with no named beneficiary |
| **Risk reduction** | "Removes single point of failure that caused incident #123; estimated blast radius reduced from N customers to M" | "More robust" with no failure mode named |
| **Developer velocity** | "Removes a manual step that costs ~2 eng-hours/week across the team, per time-tracking/retro data" | "Better DX" with no measured friction |
| **Compliance / legal** | "Required for SOC2 control X" or "required by contractual data-residency clause in customer Y's MSA" | "Best practice" with no named requirement |

A benefit that can't be placed in one of these rows with a number or a named concrete
beneficiary is not yet a benefit — it's a hope. That's fine as a note, but it shouldn't
carry weight in the recommendation until it's sharpened.

---

## 5. Scoring the tradeoff

Once costs and benefits are itemized, use whichever of these fits the decision's stakes —
don't reach for more rigor than the decision warrants.

### 5.1 Lightweight: threshold check (most decisions)
Ask three questions in order. A "no" at any step ends the analysis — don't proceed to
build a spreadsheet for a decision that fails here:

1. **Is the problem statement real and measured?** (Section 2.1) If not, go measure first.
2. **Does the cheapest option that would fix it get ruled out with a stated reason?**
   (Usually: "do nothing" or "the boring/incremental fix" — see Section 2.2.) If the
   cheap option isn't explicitly ruled out, it's the default answer.
3. **Do the named, quantified benefits (Section 4) plausibly exceed the six-category cost
   (Section 3) within a stated time horizon?** "Plausibly" is fine — this isn't NPV
   modeling, it's a sanity check that someone did the arithmetic.

### 5.2 Heavier: weighted scoring (large/contested decisions)
When stakes are high enough that people reasonably disagree (Section 1's second bullet),
score each option 1-5 on the dimensions that matter for *this* decision (commonly: cost,
time-to-value, risk, reversibility, team familiarity), weight the dimensions by what
actually matters here, and total it. The number isn't the decision — it's a forcing
function that makes disagreements concrete ("we disagree on the risk weight," not "I
don't like this") and gives the deciding group something specific to argue about instead
of re-litigating the whole proposal.

### 5.3 What scoring is not for
Never use a score to manufacture false precision on a decision that's actually about risk
appetite or strategic bet-placing. If the real disagreement is "are we willing to bet 3
months on this," say that plainly in the recommendation instead of hiding it inside
scoring weights.

---

## 6. Decision record

Every decision made through this process gets a short, durable record — this is what
makes the process worth the overhead: it prevents the same debate from happening again in
eight months with no memory of why the prior call was made.

```markdown
# Decision: <short title>

Date: <date>          Status: proposed | accepted | superseded by <link>
Owner: <name>          Approved by: <name(s)/forum>

## Problem
<Section 2.1, 2-4 sentences>

## Options considered
1. <option> — cost: <summary>, benefit: <summary>
2. <option> — cost: <summary>, benefit: <summary>
3. Do nothing / defer — cost: <summary>

## Decision
<chosen option, and the one or two sentences of reasoning that actually drove it —
not a restatement of all the analysis above>

## What would change this decision
<the specific future signal that would trigger revisiting this — e.g. "if write volume
exceeds X/sec" or "if this pattern needs to be copied by a third team">

## Review date
<when this gets revisited regardless — see Section 8>
```

Keep it to one page. The analysis in Sections 2-5 is the working material; the decision
record is the artifact that outlives the meeting where it was made. Store it wherever the
team already stores durable docs (ADR folder, wiki, this repo) — consistency of *location*
matters more than which location.

---

## 7. Anti-patterns to catch in review

These are the specific ways this process gets subverted — watch for them when reviewing
someone else's justification, and self-check for them before submitting your own:

- **Solution-shaped problem statement.** ("We need Kafka" is not a problem statement.)
  Forces Section 2.2 to only ever produce one real option.
- **Missing "do nothing."** If it's not on the list, the analysis is a pitch, not a
  comparison.
- **Unnamed benefits.** "Better," "more scalable," "modern," "cleaner" with no number and
  no named beneficiary. Send it back to Section 4.
- **Cost estimate that's build-cost only.** No run/migration/exit cost named — almost
  always means those costs weren't considered, not that they're zero.
- **No reversal criteria.** If nobody can say what would make the team undo this later,
  it's being treated as low-stakes when it's actually a one-way door.
- **Scoring used to end a disagreement about risk appetite.** A weighted score that
  conveniently favors the proposer's preferred option, presented as if the math settled
  a question that was actually about how much risk the org is willing to take.
- **No review date.** Decisions made under conditions that will change (traffic growth,
  team size, a vendor's pricing) need a forced revisit, not an assumption that "we'll
  notice if it stops making sense."

---

## 8. Review cadence

Decision records aren't permanent. Set a review date at decision time (Section 6) based on
what would invalidate the decision:

- **Growth-driven decisions** (scaling, sharding, capacity): review when the triggering
  metric is forecast to be hit, not on a fixed calendar — check quarterly whether the
  forecast has moved.
- **Compliance-driven decisions**: review on the underlying requirement's cycle (audit
  cadence, contract renewal).
- **Bet-driven decisions** (new framework, new pattern): review at a fixed horizon (6-12
  months) regardless of triggers — bets need a checkpoint even if nothing obviously broke.

A decision with no review date effectively becomes permanent by default, which is fine for
truly foundational choices and wrong for everything made under uncertainty.

---

## 9. Escalation — who approves what

Calibrate approval authority to blast radius and reversal cost, not to raw dollar cost
alone — a cheap decision that's hard to reverse deserves more scrutiny than an expensive
one that's trivial to undo.

| Signal | Approval level |
|---|---|
| Team-local, reversible, no cross-team dependency | Team lead / tech lead sign-off, record kept for institutional memory |
| Crosses team boundaries, or sets a pattern other teams will likely copy | Cross-team review (staff/principal forum, architecture review) |
| New vendor/infra the org will pay for and operate long-term | + platform/infra org, + whoever owns the budget line |
| Touches compliance, data residency, or security posture | + security/compliance sign-off, non-negotiable regardless of technical merit |
| Effectively irreversible within 1 year (major data migration, contractual lock-in) | + the level above whoever's approving — this should feel like a deliberately higher bar |

The point of tiering is to keep the lightweight path (Section 5.1) actually lightweight for
small decisions, while making sure decisions that are hard to undo get looked at by more
than one person before they're locked in.

---

## 10. Worked example

`spring-postgres/sharding.md` in this repo applies this framework to a concrete technical
question — "should we shard this Postgres database" — including a purpose section (maps
to Section 2.1 here), a cost/benefit table (Sections 3-4), an explicit decision gate
(Section 5.1's threshold check, applied), and a checklist that mirrors Section 9's
escalation thinking for an infra-level, hard-to-reverse decision. Use it as the reference
for how much rigor a real decision under this framework should have — thorough enough to
survive being questioned six months later, not so exhaustive it becomes a document nobody
reads before deciding anyway.