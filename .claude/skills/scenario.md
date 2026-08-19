---
name: scenario
description: Add a new Gherkin scenario to an existing feature file and stub the missing step definition methods
---

Add one new Gherkin scenario to an existing `.feature` file, then stub any unmatched steps in the corresponding Java step definitions class.

## Usage

```
/scenario [module] [feature-file] [scenario description]
```

- `module` — module directory, e.g. `spring-bdd` (default: `spring-bdd`)
- `feature-file` — name of the `.feature` file without path, e.g. `user.feature`
- `scenario description` — plain-English description of the new test case

If any argument is missing, list the available feature files and ask.

---

## Steps

### 1. Read existing feature file

Read `<module>/src/test/resources/features/<feature-file>` in full. Note:
- The existing `Given`/`When`/`Then` step texts already defined
- The naming style and value format (`{string}`, `{long}`, `{double}`)

### 2. Read the matching step definitions class

Find the `*StepDefinitions.java` in `<module>/src/test/java/` whose name matches the feature file (e.g. `user.feature` → `UserStepDefinitions.java`). Read it in full. Note every step annotation text already implemented.

### 3. Write the new scenario

Append to the feature file. Rules:
- Blank line before the new `Scenario:` block
- Mirror the step phrasing style of existing scenarios
- Use quoted values for strings, plain numbers for numerics
- Cover one concrete case — not multiple edge cases in one scenario
- Do not add tags unless the user asked for them

### 4. Identify missing steps

Compare every `Given`/`When`/`Then`/`And` line in the new scenario against the annotations already in the step definitions class. Collect only the steps that have no matching annotation.

### 5. Stub missing step methods

Append stub methods to the step definitions class for each unmatched step. Rules:
- Use the correct annotation (`@Given`, `@When`, `@Then`, `@And`)
- Extract `{string}` / `{long}` / `{double}` placeholders from the step text; add matching typed parameters
- Body: `throw new io.cucumber.java.PendingException();`
- No Javadoc; no explanatory comments
- Place stubs at the end of the relevant section (Given / When / Then) matching the file's existing layout

### 6. Run the test

```bash
cd <module> && ./gradlew test --tests "*.CucumberTest"
```

Expected result: the new scenario shows as **pending** (yellow), not failed (red). Pending means Cucumber found and matched every step — the stubs just need implementation.

Report:
- **Pending** — "Scenario wired up. Implement the stub methods to make it pass."
- **Step undefined** — a step was not matched; show the unmatched step text and fix the annotation.
- **Compilation error** — show the error and fix it before re-running.