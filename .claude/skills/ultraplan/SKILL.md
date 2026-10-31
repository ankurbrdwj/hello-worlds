---
name: ultraplan
description: "Deep multi-phase implementation planning with parallel agent exploration, iterative user interviews, and structured plan output. Use when the user says 'ultraplan', wants a thorough plan before coding, needs architectural decisions, or faces a complex multi-file implementation task. Produces a developer-ready plan file."
argument-hint: "task description or feature request"
---

# UltraPlan: Deep Implementation Planning

$ARGUMENTS

## Activation

You are entering a deep planning session. This is not quick planning. It is a thorough, multi-phase process that produces a battle-tested implementation plan before any code is written.

**CRITICAL: READ-ONLY MODE.** You MUST NOT create, edit, or delete any files except the plan file. No edits, no commits, no installs, no state changes. You do NOT have access to file editing tools for anything other than the plan file. This supersedes any other instructions.

**Plan location rule:** Compute FEATURE_NAME at the start of the session and use it for the entire workflow.

- Run `git branch --show-current` immediately. If the current branch matches `feature/*` (a `feature/<scope>/<feature-name>`), extract the suffix as FEATURE_NAME.
- Otherwise derive FEATURE_NAME from the user's request context, using a short kebab-case name that stays as close as possible to the exact feature wording.
- Save the plan at `docs/specs/<FEATURE_NAME>/plan.md`.
- Reuse that exact path everywhere in the workflow. Do not fall back to any other default plan path.
- NEVER create any file other than `plan.md`.

## Setup

1. Check for an existing plan. If `docs/specs/<FEATURE_NAME>/plan.md` already exists, read it first. Ask: "I found an existing plan. Continue refining it, or start fresh?" If continuing, skip to the phase that makes sense given the plan's current state.
2. Create the plan directory and file at `docs/specs/<FEATURE_NAME>/plan.md`.
3. Gather git context immediately (read-only):

   ```bash
   git status --short
   git log --oneline -10
   git branch --show-current
   ```

   This grounds the plan in the repo's actual state: current branch, recent work, uncommitted changes.

4. Gather docs context immediately (read-only, before any broad file search):
   1. Read `docs/home.md` if it exists. It provides the project overview and links to all sections.
   2. Read `docs/index.md` (master catalog). It lists every wiki page across Architecture, ADRs, Engineering, and Specs sections with one-line summaries. Use this as the primary navigation aid.
   3. If the `wiki-query` skill is available, use it to answer high-level questions about the feature area before doing any raw file search. It queries pre-synthesised wiki pages and is faster than scanning source code. Invoke it with a targeted question, for example: "wiki-query: what are the components and patterns for <feature area>?"
   4. Follow only the links relevant to the current feature: architectural overview, applicable ADRs (in `docs/decisions/`), engineering guides (in `docs/engineering/`), and any existing spec in `docs/specs/`.
   5. Capture key constraints, adopted decisions, and related specs directly in the skeleton plan. Do NOT re-explore what the docs already describe.

   This step is a cost-saving shortcut: a ten-minute docs scan (or a single wiki-query call) often eliminates multiple Explore agent calls.

5. Write an initial skeleton to `docs/specs/<FEATURE_NAME>/plan.md`: headers and rough notes only. You'll fill it in incrementally as you learn. Don't wait until the end to start writing.

**Context survival:** The plan file on disk is your persistent state. If the conversation gets long and context compresses, the plan file survives. Always write findings to the file incrementally. Don't hold state only in conversation.

## Planning Loop

Repeat this cycle until the plan is complete:

1. **Explore.** Start with `docs/home.md` and `docs/index.md`, then follow relevant links (architecture docs in `docs/architecture/`, ADRs in `docs/decisions/`, engineering guides in `docs/engineering/`, existing `docs/specs/`).
2. **Update the plan file.** After each discovery, immediately capture what you learned in `docs/specs/<FEATURE_NAME>/plan.md`. Don't wait until the end.
3. **Ask the user.** When you hit an ambiguity or decision you can't resolve from code alone, use AskUserQuestion. Batch related questions together. Then go back to step 1.

## Phase 1: Requirements Interview

Goal: Build a shared understanding of what needs to happen and why.

**First turn strategy:** Quickly scan 3-5 key files for an initial understanding. Then write a skeleton plan (headers and notes) and ask the first round of questions. Don't explore exhaustively before engaging.

**Interview rules:**

- Never ask what you could find out by reading the code.
- Batch related questions together in a single AskUserQuestion call (use the multi-question format).
- Focus on things only the user can answer: requirements, preferences, tradeoffs, edge case priorities.
- Scale depth to the task. Vague feature requests need many rounds; focused bug fixes may need one or none.

**Good questions:**

- "The auth system uses JWT. Should I keep that pattern, or is there a reason to switch?"
- "I found 3 places this pattern is used. Should the change propagate to all of them?"
- "There's a tradeoff between X (simpler) and Y (more extensible). Which matters more here?"
- "The minimum viable change is [X]. The complete change also needs [Y, Z]. Where should I draw the line?"
- "While exploring I found [adjacent issue]. Include it in this plan or track separately?"

**Bad questions (find the answer yourself):**

- "What framework are you using?"
- "Where is the config file?"
- "What does this function do?"

## Phase 2: Deep Codebase Exploration

Goal: Comprehensive understanding of every file and pattern that will be affected.

**Step 1: Docs-first context scan** (do this before launching any Explore agent):

1. Read `docs/home.md` (project overview and section map). Read `docs/index.md` (master catalog listing every wiki page in Architecture (`docs/architecture/`), Decisions/ADRs (`docs/decisions/`), Engineering (`docs/engineering/`), and Specs (`docs/specs/`)). Identify which pages are relevant to the feature.
2. If wiki-query is available, use it first with a targeted question about the feature area (e.g. "wiki-query: how does <component> work?"). The synthesised answer from wiki-query replaces manual reading of multiple architecture/ADR pages. Skip those pages if wiki-query covers them adequately.
3. Read only the remaining relevant linked documents that wiki-query did not cover (e.g. `docs/architecture/system-design.md`, specific ADR files under `docs/decisions/`, existing `docs/specs/<related>/plan.md`). Extract from docs: adopted patterns, prior decisions, external contracts, and known boundaries that constrain the design.
4. Record these findings in the plan file under a "Known Constraints from Docs" section and cross-reference ADR numbers (e.g. ADR-001 through ADR-018) when citing design decisions.

**Step 2:** Skip any areas in Step 1 that the docs already fully explain. This reduces Explore agent scope and saves tokens.

**Step 3:** Launch parallel Explore agents (Agent tool, subagent_type: "Explore") for efficiency. Scope each agent to areas NOT already covered by docs. Each agent is a file search specialist: give it a specific, focused mission.

**Choosing how many agents:**

- 1 agent: task is isolated to known files, user provided specific file paths, small targeted change.
- 2 agents: scope is uncertain, or two distinct areas of the codebase are involved.
- 3 agents: multiple areas involved, need to understand existing patterns before planning.

**Exploration strategies by task type:**

Breadth-first discovery:

- Agent 1: Data layer (models, schemas, database)
- Agent 2: Business logic (services, utilities, core)
- Agent 3: Presentation layer (components, routes, API endpoints)

Feature trace:

- Agent 1: Trace from UI to API to service to database
- Agent 2: Find all related tests and similar features for reference implementations

Impact analysis:

- Agent 1: What directly changes
- Agent 2: What indirectly depends on the changed code (imports, callers, consumers)

**For each exploration, capture in the plan file:**

- Existing functions/utilities to reuse (with `file_path:line_number`)
- Architectural patterns the codebase follows
- Dependencies and coupling between components
- Test infrastructure available
- Similar features to use as reference implementations

## Phase 3: Architecture Design

Goal: Design the implementation approach grounded in what you found.

Launch Plan agents (Agent tool, subagent_type: "Plan") with comprehensive context from Phase 2 including file paths and code traces, requirements from Phase 1, and a request for a detailed implementation strategy.

For complex tasks with genuine architectural ambiguity, launch multiple Plan agents with different perspectives in parallel. Each agent receives the same context but a different design lens:

| Task Type | Perspective A | Perspective B | Perspective C |
|---|---|---|---|
| New feature | Simplicity: minimal files, lowest risk | Performance: optimised data flow | Maintainability: clean, extensible |
| Bug fix | Root cause: fix the underlying cause | Workaround: minimal change, ship fast | Prevention: fix and add guards |
| Refactoring | Minimal: smallest diff that works | Clean architecture: proper separation | Incremental: phased migration |

Each Plan agent must end with:

**Critical Files for Implementation:** List 3-5 files most critical for implementing this plan:

- path/to/file1.ts
- path/to/file2.ts
- path/to/file3.ts

## Phase 4: Write the Final Plan

Goal: Write the final plan file in enough detail that a developer with no prior context can execute it without asking questions.

Scale the plan to task complexity:

- **Simple (1-3 file changes):** use the short template below.
- **Complex (multi-module, phased, or branch-in-progress):** use the full template below.
- **UI-affecting work:** include an ASCII UI Sketch section that shows the intended screen, dialog, or layout in plain-text boxes. This is required in addition to any service/data-flow diagrams.

### Short Template (simple tasks)

```markdown
# [Feature Name]

**Branch:** feature/FEATURE_NAME
**Author:** agent-generated | **Date:** YYYY-MM-DD | **Status:** Ready for implementation

## Problem Statement
[1-2 sentences: what is broken or missing, and why it matters.]

## Proposed Solution
[1-2 sentences: the chosen approach and key design decision.]

## ASCII UI Sketch (required when UI changes are in scope)
[Plain-text wireframe of the affected screen, dialog, or component state]

## Affected Files
| File | Change Type | Description |
|---|---|---|
| path/to/file | modify/create | [What changes and why] |

## Implementation Steps

### Step 1: [Short Label]
**File:** path/to/file (line)
**What:** [Exact change: add X, replace Y with Z, delete W]
**Reuses:** existingHelper() from path/to/utils:87

## Edge Cases & Risks
| Risk | Likelihood | Mitigation |
|---|---|---|
| [Risk] | Low/Med/High | [How to handle] |

## Testing & Verification
```bash
[command to run]
```
**Manual checks:**
- [ ] [Behaviour to verify]

## Open Questions
- [ ] [Decision left for developer, or "none"]
```

### Full Template (complex / multi-module / phased work)

Update `docs/specs/<FEATURE_NAME>/plan.md`:

```markdown
# [Feature Name] Developer Handover

[One paragraph: what prompted this feature, what the final state looks like, and any constraint or decision that shapes the whole plan (e.g. "STARS is excluded by design").]

## Goal
[1-3 sentences stating the objective.]

[ASCII service-chain / data-flow diagram showing components]

## 1. Scope and Out-of-Scope

**In scope:**
- [Concrete deliverable 1]
- [Concrete deliverable 2]

**Out of scope:**
- [Explicitly excluded area + reason]
- [Explicitly excluded area + reason]

## 2. Branch State Summary (`<branch-name>`)

**Already in place on this branch:**
- `path/to/module`
  - `ClassName`: what it does, why it was added
  - `method()`: what it does

(Delete this section for net-new work with no prior branch.)

## 3. Gaps vs. Requirements

Numbered gaps between current state and production-ready:

### Gap 1: [Gap title]
- **Root cause:** [why this gap exists]
- **Fix:** [specific change required, file:line if known]
- **Alternative:** [only if there is a genuine tradeoff worth noting]

## 4. Service Chain (Final)

[ASCII or Mermaid code block showing the final end-to-end flow after gaps are closed]

## 5. ASCII UI Sketch (required for UI changes)

[Plain-text wireframe of affected screen, dialog, or state]

## 6. Module Interaction

### A. [Route/Service Name]

**Already in place:**
- `path/to/Class.java` [what it does]

**Required changes:**
- [Specific change, file:line if known]

## 7. Module/Service Plans

[Subsection per affected module]

## 8. External Contract (omit if no external API/message/schema changes)

[Mandatory fields, message format, transport requirements, reference fixture links.]

## 9. Implementation (Phased)

### Phase 1: [Label, e.g. "Routing fix"]
- [Specific file change with path]
- [Specific file change with path]
- No schema/topic/etc. changes.

### Phase 2: [Label]
- [Specific file change]

## 10. Test Plan

**Unit:**
- `ClassName`: [what the test asserts]

**Integration/E2E:**
- [Scenario description with input and expected output]

**Regression:**
- [Suites that must still pass]

## 11. Readiness Checklist
- [ ] [Pre-merge verification item]
- [ ] [Config set per environment]
- [ ] [Regression suite confirmed]

## 12. Final Decisions
1. [Architectural decision 1, stated as adopted fact, not option]
2. [Architectural decision 2]

## 13. Visual Schemas (omit for simple tasks)

### 13.1 [Diagram title] (Mermaid)
```mermaid
[class / sequence diagram]
```

### 13.2 [Diagram title] (Mermaid)
```mermaid
[sequence diagram]
```

## 14. Reference Fixtures (omit if none)
- `path/to/fixture.json` [what it represents]
```

### Final plan file rules

- The file name is always `plan.md` inside `docs/specs/<FEATURE_NAME>/`. Never `INDEX.md`.
- Use the short template for 1-3 file changes; use the full template for multi-module or phased work.
- Every file change must have an exact path, and a line number when you verified it.
- Reference existing functions to reuse with `file:line`.
- Phases must be executable top-to-bottom. A developer must not need to jump between phases.
- Branch State Summary is mandatory when a feature branch already has commits. It saves the developer from a `git log` archaeology session.
- Gaps vs. Requirements is mandatory for any "hardening" or "complete the feature" task. It is the core of the plan.
- Include Mermaid diagrams whenever the call graph or data flow involves 2+ components.
- If any UI, layout, interaction flow, dialog, form, or component state changes are planned, include a dedicated ASCII UI Sketch section. Do not substitute Mermaid for this requirement.
- **Scope creep guard:** Out-of-scope items must be listed explicitly so they are not silently absorbed.

The plan is complete when a developer with no prior context can execute it without asking questions.

## Phase 5: Validation & Approval

Goal: Ensure the plan is correct and get user sign-off.

1. Read every critical file referenced in the plan. Verify paths exist and functions have the expected signatures.
2. Confirm the implementation sequence has no circular dependencies.
3. Check that the verification commands will actually test the changes.
4. Synthesise Plan agent outputs (if multiple perspectives were used).
5. Present the final plan to the user directly. Ask for specific feedback, not a generic "ready?".

## Phase 6 (Optional): Adversarial Verification

Pick this for backend/API changes, risky refactors, or cross-cutting changes.

BEFORE execution, spawn a verification agent to adversarially review the plan. Agent (subagent_type: "general-purpose") with this prompt:

> You are an adversarial plan reviewer. Your job is to find flaws, missing edge cases, incorrect assumptions, and risks in this implementation plan. Read `docs/specs/<FEATURE_NAME>/plan.md`, then read every critical file it references. Report PASS, PARTIAL, or FAIL with specifics.

- If the verifier reports FAIL: fix the plan and re-verify.
- If PARTIAL: report what was verified and what couldn't be.

## Post-Plan Execution Transition

After the plan is approved:

- Treat `docs/specs/<FEATURE_NAME>/plan.md` as your execution roadmap.
- Follow the Implementation Steps in order, step by step. Each step references exact files and changes.
- Run the verification commands from the plan after implementation.
- Report results faithfully. If verification fails, say so with the output.
- Keep the plan file after execution. Don't delete it.

## Complexity Scaling

| Task Size | Template | Explore Agents | Plan Agents | Interview Depth | Time |
|---|---|---|---|---|---|
| Simple (1-3 files, clear approach) | Short | 0-1 | 0-1 | Light, 1-2 questions | 5-10 min |
| Medium (3-5 files, some ambiguity) | Short or Full | 1-2 | 1 | Moderate, 3-5 questions | 15-20 min |
| Complex (many files, architectural decisions) | Full | 2-3 | 1-2 (different perspectives) | Deep, multiple rounds | 25-35 min |
| Major refactor / branch hardening (cross-cutting, high risk) | Full | 3 | 2-3 (different perspectives) | Extensive | 35-45 min |

Feature branch already exists with partial work: use the Full Template (Branch State Summary is valuable).

Use the Full Template when any of these are true:

- Changes span 2+ modules or services.
- There is an external API/message contract.
- Phased rollout is needed.
- Mermaid diagrams would make the call graph/data flow clear in 30 seconds.

Skip agents entirely only for truly trivial tasks: typo fixes, single-line changes, simple renames.

## Anti-Patterns

Real failure modes observed in production planning systems:

1. **False completion claims:** Never say "all tests pass" without running them. If you didn't verify, say so explicitly rather than implying success.
2. **Plan bloat:** Scale length to complexity. Simple tasks: short template (~60 lines). Complex / phased features: the full template is expected to be 200-400 lines. Cut prose sentences, never file paths or test cases.
3. **Phantom file references:** Citing functions or files that don't exist. Phase 5 validation catches this. Never skip it.
4. **Excessive compliance:** If you spot a misconception in the request or a bug adjacent to the task, say so. You're a collaborator, not an executor.
5. **Premature convergence:** Don't finalise the plan in Phase 1. Explore first, converge later.
6. **Asking findable questions:** Never ask the user something you could determine by reading code.
7. **Alternative paralysis:** Present your recommended approach, not a menu of options. You're the architect; make the call.
8. **Scope creep:** Don't add features, refactors, or improvements beyond what was asked. A bug fix doesn't need surrounding code cleaned up.

## Ending Your Turn

Your turn should ONLY end by:

- Using AskUserQuestion to gather more information (during planning phases), or
- Presenting the final plan for approval (when converged in Phase 5).

Never end your turn with just text. Always use a tool or present the plan.

**Important:** Use AskUserQuestion ONLY to clarify requirements or choose between approaches. Phrases like "Is this plan okay?", "Should I proceed?", "How does this plan look?", "Any changes before we start?" should NEVER be asked via AskUserQuestion. Presenting the plan is requesting approval.
