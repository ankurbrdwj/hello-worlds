package com.ankur.bdd.steps;

import com.ankur.bdd.exception.DuplicateResourceException;
import com.ankur.bdd.model.User;
import com.ankur.bdd.repository.UserRepository;
import com.ankur.bdd.result.Result;
import com.ankur.bdd.result.UserError;
import com.ankur.bdd.service.UserService;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the User Management feature.
 *
 * Each method maps to one Gherkin step via the @Given/@When/@Then/@And annotations.
 * The {string} placeholder in the annotation captures a quoted string from the feature file.
 *
 * State is held in instance variables and reset after each scenario via @After.
 */
public class UserStepDefinitions {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // ── Scenario-scoped state ─────────────────────────────────────────────────
    private User createdUser;
    private Optional<User> foundUser;
    private boolean creationFailed;
    private String failureReason;

    // ── Cleanup after each scenario ───────────────────────────────────────────

    @After
    public void cleanUp() {
        userRepository.deleteAll();
        createdUser = null;
        foundUser = null;
        creationFailed = false;
        failureReason = null;
    }

    // ── Given steps ───────────────────────────────────────────────────────────

    @Given("no user exists with email {string}")
    public void noUserExistsWithEmail(String email) {
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
    }

    @Given("a user already exists with email {string} and name {string}")
    public void aUserAlreadyExistsWithEmailAndName(String email, String name) {
        if (!userRepository.existsByEmail(email)) {
            userService.createUser(email, name);
        }
    }

    @And("the user with email {string} is deactivated")
    public void theUserWithEmailIsDeactivated(String email) {
        User user = userService.getByEmail(email);
        userService.deactivateUser(user.getId());
    }

    // ── When steps ────────────────────────────────────────────────────────────

    @When("I create a user with email {string} and name {string}")
    public void iCreateAUserWithEmailAndName(String email, String name) {
        createdUser = userService.createUser(email, name);
    }

    @When("I try to create another user with email {string} and name {string}")
    public void iTryToCreateAnotherUserWithEmailAndName(String email, String name) {
        try {
            userService.createUser(email, name);
        } catch (DuplicateResourceException e) {
            creationFailed = true;
            failureReason = e.getMessage();
        }
    }

    @When("I look up the user by email {string}")
    public void iLookUpTheUserByEmail(String email) {
        foundUser = userService.findByEmail(email);
    }

    @When("I deactivate the user with email {string}")
    public void iDeactivateTheUserWithEmail(String email) {
        User user = userService.getByEmail(email);
        userService.deactivateUser(user.getId());
    }

    @When("I activate the user with email {string}")
    public void iActivateTheUserWithEmail(String email) {
        User user = userService.getByEmail(email);
        userService.activateUser(user.getId());
    }

    // ── Then / And steps ──────────────────────────────────────────────────────

    @Then("the user should be created successfully")
    public void theUserShouldBeCreatedSuccessfully() {
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
    }

    @Then("the user's name should be {string}")
    public void theUserSNameShouldBe(String expectedName) {
        if (createdUser != null) {
            assertThat(createdUser.getName()).isEqualTo(expectedName);
        } else {
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getName()).isEqualTo(expectedName);
        }
    }

    @And("the user should be active by default")
    public void theUserShouldBeActiveByDefault() {
        assertThat(createdUser.isActive()).isTrue();
    }

    @Then("the creation should fail with a duplicate email error")
    public void theCreationShouldFailWithADuplicateEmailError() {
        assertThat(creationFailed).isTrue();
        assertThat(failureReason).contains("already exists");
    }

    @Then("the user should be found")
    public void theUserShouldBeFound() {
        assertThat(foundUser).isPresent();
    }

    @Then("the user should not be found")
    public void theUserShouldNotBeFound() {
        assertThat(foundUser).isEmpty();
    }

    @Then("the user should be inactive")
    public void theUserShouldBeInactive() {
        // Reload from DB to get the persisted state
        User user = foundUser != null && foundUser.isPresent()
                ? userRepository.findById(foundUser.get().getId()).orElseThrow()
                : userRepository.findAll().stream()
                        .filter(u -> !u.isActive())
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("No inactive user found"));
        assertThat(user.isActive()).isFalse();
    }

    @Then("the user should be active")
    public void theUserShouldBeActive() {
        User user = userRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("No user found"));
        assertThat(user.isActive()).isTrue();
    }
}