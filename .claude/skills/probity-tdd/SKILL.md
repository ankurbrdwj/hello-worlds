---
name: probity-tdd
description: Self-enforced Red-Green-Refactor gate mirroring probity's enforceTdd rule. Use before every write to Java source or test files — judge the change against the cycle and block yourself, with a named reason and the next legal step, instead of writing code that skips it.
---

# Probity-style TDD gate

Probity (github.com/nizos/probity) enforces this discipline for agents at the
hook level, blocking the write outright. This repo has no Node toolchain and
no ast-grep matcher for Java, so there is no hook to do that here. This skill
is the same rule set, self-applied: before any write to `src/main/**` or
`src/test/**`, judge the change against the rules below. If it would violate
the cycle, do not make the write — say so out loud in the same format probity
uses, then take the legal step instead.

## What you judge

Judge the **change** the write makes (diff between current file content and
what you're about to write), not the resulting file as a whole.

A transient broken state is never itself a violation: an unresolved symbol, a
dead or unused definition, a half-finished multi-step change. Whether the
file compiles or the suite is green is checked when you next run
`./gradlew test`, not here. This allowance is about structure — it doesn't
excuse skipping a failing test or over-implementing.

A phase can span multiple writes, each fine on its own — e.g. add an import
in one write, wire the calling code in the next; add a method signature in
one write, its body in another.

## The rules

**Across all phases** — deleting code, tests, or helpers never needs a
failing test to drive it, even if the removed code was covered.

### Red — write a failing test first

- One write adds at most one new test. Compare current vs. pending content;
  existing tests don't count, and restructuring existing tests isn't adding.
- Adding a test is the red step itself — allowed without observing it fail
  first, *unless* the prior green left a refactor unmade (see below).
- A test driving new behavior must be observed failing for the right reason
  (an assertion, not a compile error) before you write production code to
  satisfy it.
- A test capturing *existing* behavior (characterization test, a test at a
  new layer, a pinning test ahead of a refactor) may pass immediately — don't
  block on it.
- Test scaffolding (imports, fixtures, test helpers) needs no failing test of
  its own.

**Reaching a clean red** — a test can fail before reaching its assertion
(unresolved symbol, signature mismatch). Resolving that is legal without
counting as implementation:

- Unresolved symbol → create a placeholder stub: a body that makes the
  symbol exist but doesn't implement the asserted behavior (`throw new
  UnsupportedOperationException()`, or a literal the assertion rejects).
- Signature mismatch → adjust the signature, keep the body a stub.
- Only once you have an assertion failure → implement the minimum to pass.

A stub must never satisfy the assertion it's stubbing. Returning a literal
the test would accept is not a stub — it's the implementation, out of order.

### Green — minimum to pass

The implementation must not exceed what the currently-failing test requires.
A method, class, or branch not needed by that test is over-implementation.
Scaffolding awaiting a later write in the same change is transient, not
over-implementation.

### Refactor — improve structure under green

Doesn't need a failing test. Legal when the tests it touches were passing
before you started:

- Extracting a helper whose behavior already lives elsewhere (covered by
  existing tests). A helper whose behavior is net new needs a failing test
  first.
- Lifting test setup (builders, fixtures) into a shared helper — no separate
  test needed for the helper itself.
- Renaming, restructuring control flow, removing dead code, adding
  interfaces/constants with no runtime behavior.
- Reorganizing, splitting, or combining existing tests — the one-new-test
  rule is about new behavior, not diff size.

**Enforcing it**: writing the next test crosses the green→red boundary, so
this check applies there too, not just to production writes. When the prior
green left a refactor that's unmistakable and clearly without downside,
don't write the next test yet — do the refactor first. The bar is high:
forcing a refactor that isn't clearly a win risks needless abstraction or
breaking a convention you can see and shouldn't second-guess. When it's not
clear-cut, let green stand and move on.

## When a write would violate this

Stop before making it. State it the way probity does — name the violation,
say why it breaks the cycle, then give the next legal step. Don't dictate
unrelated edit order or demand the file be complete/runnable.

```
Probity: you're adding production code before a failing test has been
observed.

The next TDD-legal step is to add one focused test in
OrderServiceTest.java and run it to a clean assertion failure before
implementing only the minimum code to pass it.
```

Then take that step instead of the blocked one.

## Java note

There's no automated "count new `@Test` methods in this diff" check here
(that's what probity's ast-grep matcher does for JS/TS/Python/C#/Ruby/PHP —
Java isn't in that list). Do it by eye: diff the file, count new
`@Test`-annotated methods added versus removed/restructured. If that ever
needs to be exact rather than judgment-based, a small script (e.g. `git diff
-- '*.java' | grep -c '^+.*@Test'`) could be added — not needed for this
skill today, so it isn't included.