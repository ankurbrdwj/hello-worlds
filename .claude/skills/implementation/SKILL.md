---
name: implementation
description: Orchestrate spec-driven feature delivery by delegating backend and frontend work in parallel, routing the result through code review, then validating the implementation in dev with Playwright and saving a summary.md alongside the spec.
argument-hint: "spec path or implementation request"
metadata:
  tags: implementation, orchestration, backend, frontend, code-review, playwright, validation
  platforms: Claude
---

# Implementation Orchestration

## Task

$ARGUMENTS

## When to Use This Skill

Use this skill when:

- A spec or plan already exists and now needs to be implemented.
- The work spans backend and frontend layers.
- The implementation should be delegated to specialised agents instead of being coded directly in the main thread.
- The result must be reviewed, validated in dev mode, and written up in `summary.md`.

## Operating Mode

You are the orchestrator, not the implementer.

- Do not write the feature code yourself.
- Delegate implementation to the proper agents.
- Keep backend and frontend workstreams moving in parallel.
- Collect results, resolve review feedback, and drive the task to a verified finish.

## Required Inputs

Prefer a concrete spec or plan path, for example:

- `docs/specs/my-feature/plan.md`
- `docs/specs/my-feature/`

If the user gives only a feature request, first locate the relevant spec or ask for it.

## Required References

Before delegating, read and pass along the relevant project references:

- `docs/home.md`: project overview and section map
- `docs/index.md`: master catalog of all wiki pages (Architecture, ADRs, Engineering, Specs); use this to identify which docs are relevant instead of scanning blindly
- `docs/architecture/`: pages relevant to the feature (navigate via `docs/index.md`)
- `docs/decisions/`: ADRs relevant to the feature (navigate via `docs/index.md`)
- `docs/engineering/`: guides relevant to the feature (navigate via `docs/index.md`)
- The target spec or plan

If the `wiki-query` skill is available, use it before reading individual pages. A single wiki-query call over the feature area typically replaces reading 3-5 separate architecture/ADR pages and should be the first context-gathering step.

Use other project skills when they materially help execution, especially architecture, testing, reporting, or React best-practices guidance.

## Workflow

### 1. Prepare the implementation brief

Gather docs context first:

1. Read `docs/index.md` to identify relevant architecture pages, ADRs, and engineering guides for the feature.
2. If wiki-query is available, invoke it with the feature area as the query. Use the synthesised answer as the primary architecture/context input and skip reading individual pages that wiki-query already covers. Read only the remaining relevant pages not covered by wiki-query.
3. Read the spec and identify backend scope, frontend scope, acceptance criteria, and expected summary location.

Then:

- Determine the summary target as `summary.md` in the same folder as the spec or plan.
- Split the work into backend and frontend tracks with clear file-level goals.

### 2. Delegate implementation in parallel

Launch the specialised agents in parallel:

- **backend-developer** for API, workers, services, database, migrations, and Python logic
- **frontend-developer** for React pages, components, styling, and UI behaviour

Each agent brief must include:

- The exact spec or plan path
- Relevant architecture or design-system references
- The acceptance criteria it owns
- The files or layers it is expected to touch
- A request to report changed files, key decisions, and anything blocked

Do not serialise backend and frontend implementation unless one truly depends on the other.

### 3. Consolidate results

- Review each agent result.
- If one agent is blocked by the other, send a focused follow-up and continue coordinating.
- Keep the implementation aligned with the spec rather than letting either track drift.

### 4. Hand over to code review

Once implementation is complete, hand the changes to the code-review agent.

The review must focus on:

- Correctness against the spec
- Cross-layer integration gaps
- Security and data-handling issues
- Maintainability and missing validation

### 5. Route review feedback back to owners

- Send backend findings to the backend-developer agent.
- Send frontend findings to the frontend-developer agent.
- Repeat until blocking review findings are resolved.

Do not skip this loop if the reviewer finds real issues.

### 6. Validate in dev mode

Run the application in dev mode and validate the implementation against the acceptance criteria, using Playwright for the UI flows. Record the steps executed and the outcome.

### 7. Write the implementation summary

Save implementation and validation results to `summary.md`.

The summary should include:

- What was implemented
- Which backend and frontend files changed
- Review findings that mattered and how they were resolved
- Validation steps executed
- Validation outcome, including any remaining known issues

### 8. Send the required next-step recommendation to the user

After implementation is fully completed (code, review loop, validation, summary), use this exact format:

```text
Next steps:
- Compact the conversation with /compact before follow-up requests.
- Or continue in a new session.
```

## Behavioural Rules

- Stay in manager mode throughout the implementation.
- Prefer parallel delegation over single-threaded execution.
- Provide agents with full context, not abbreviated prompts.
- Use the repo's existing scripts, docs, and conventions.
- Do not claim completion until implementation, review, validation, and `summary.md` are all done.

## Example Trigger Phrases

- "Implement docs/specs/scenario-x/plan.md"
- "Use the implementation skill for this spec and ship it with agent orchestration"
