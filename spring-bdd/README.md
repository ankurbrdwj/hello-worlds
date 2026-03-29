# BDD with Cucumber + Spring Boot

## What is BDD?

Behaviour-Driven Development (BDD) is a way of writing tests in plain English so that developers, testers, and non-technical stakeholders can all read and understand them. Tests describe *behaviour* from the user's point of view, not implementation details.

---

## Building Blocks

### 1. Feature File (`.feature`)

Written in **Gherkin** — a structured plain-English syntax. Lives in `src/test/resources/features/`.

```gherkin
Feature: User Management
  As a platform user
  I want to manage my account
  So that I can create price alerts

  Scenario: Create a new user successfully
    Given no user exists with email "alice@example.com"
    When  I create a user with email "alice@example.com" and name "Alice"
    Then  the user should be created successfully
    And   the user's name should be "Alice"
```

| Part        | Purpose                                              |
|-------------|------------------------------------------------------|
| `Feature`   | Groups related scenarios. Describes the capability.  |
| `Scenario`  | One concrete test case.                              |
| `Given`     | Pre-condition / system state before the action.      |
| `When`      | The action the user or system performs.              |
| `Then`      | The expected outcome to assert.                      |
| `And` / `But` | Continuation of the previous keyword.             |

---

### 2. Step Definitions (Java)

Each Gherkin step is matched to a Java method by an annotation whose value is a text expression.

```java
// UserStepDefinitions.java
@Given("no user exists with email {string}")
public void noUserExistsWithEmail(String email) {
    userRepository.findByEmail(email).ifPresent(userRepository::delete);
}

@When("I create a user with email {string} and name {string}")
public void createUser(String email, String name) {
    result = userService.createUser(email, name);
}

@Then("the user should be created successfully")
public void userCreatedSuccessfully() {
    assertThat(result.isSuccess()).isTrue();
}
```

- `{string}` captures a quoted value from the step text and injects it as a method argument.
- The method runs when Cucumber matches that step during a scenario.
- State is shared between steps using instance fields (e.g. `result`, `fetchedUser`).

---

### 3. Runner Class

Tells JUnit 5 to use the Cucumber engine and where to find feature files and step definitions.

```java
// CucumberTest.java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")                          // scan src/test/resources/features/
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.ankur.bdd")  // scan this package for steps
public class CucumberTest {}
```

---

### 4. Spring Context Bridge

Allows step definitions to use `@Autowired` beans (services, repositories, etc.).

```java
// CucumberSpringConfig.java
@CucumberContextConfiguration
@SpringBootTest
public class CucumberSpringConfig {}
```

Without this, Cucumber would not start a Spring context and `@Autowired` would be null.

---

## How It All Connects

```
JUnit 5
  └── finds @Suite on CucumberTest
        └── Cucumber engine scans features/ for .feature files
              └── each Gherkin step is matched by text to a @Given/@When/@Then method
                    └── method runs with injected arguments
                          └── step definitions use @Autowired beans (Spring context from CucumberSpringConfig)
```

---

## File Layout

```
src/
  main/java/com/ankur/bdd/
    model/          # JPA entities  (User, PriceAlert)
    repository/     # Spring Data repositories
    service/        # Business logic
    exception/      # Custom exceptions

  test/
    java/com/ankur/bdd/
      CucumberTest.java           # Runner — entry point for JUnit 5
      CucumberSpringConfig.java   # Bridges Cucumber with Spring Boot
      steps/
        UserStepDefinitions.java       # Steps for user.feature
        UserPriceAlertSteps.java       # Steps for user_price_alert.feature
      dto/                             # Request/response objects used in steps

    resources/
      features/
        user.feature                   # Scenarios for user management
        user_price_alert.feature       # Scenarios for cross-service alert creation
```

---

## Running Tests

Run all scenarios:
```bash
./gradlew test
```

Run scenarios with a specific tag:
```bash
./gradlew test -Dcucumber.filter.tags="@user-management"
```

Run a single feature file:
```bash
./gradlew test -Dcucumber.features="src/test/resources/features/user.feature"
```

Run from the IDE by clicking the green play button next to any scenario or feature in the `.feature` file.

---

## Key Dependencies (`build.gradle`)

| Dependency                            | Purpose                                      |
|---------------------------------------|----------------------------------------------|
| `cucumber-java`                       | Core Cucumber library + Gherkin step annotations |
| `cucumber-spring`                     | Integrates Cucumber with Spring context      |
| `cucumber-junit-platform-engine`      | Lets JUnit 5 discover and run Cucumber tests |
| `junit-platform-suite`                | Enables `@Suite` on the runner class         |
| `spring-boot-starter-data-jpa`        | JPA repositories and entities                |
| `h2` (testRuntimeOnly)                | In-memory database used during tests         |