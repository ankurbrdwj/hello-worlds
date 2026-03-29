package com.ankur.bdd.steps;

import com.ankur.bdd.dto.AlertRequest;
import com.ankur.bdd.dto.AlertResponse;
import com.ankur.bdd.dto.UserRequest;
import com.ankur.bdd.dto.UserResponse;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserPriceAlertSteps {

    @Value("${user-service.base-url}")
    private String userServiceUrl;

    @Value("${price-alert.base-url}")
    private String priceAlertUrl;

    @Autowired
    private RestTemplate restTemplate;

    // ── Scenario-scoped state ─────────────────────────────────────────────────
    private Map<String, Object> fetchedUser;   // raw response from user-service
    private UserResponse registeredUser;        // after registering in price-alert
    private AlertResponse createdAlert;

    // ── Cleanup after each scenario ───────────────────────────────────────────
    @After
    public void cleanup() {
        if (createdAlert != null) {
            restTemplate.delete(priceAlertUrl + "/api/alerts/" + createdAlert.id());
        }
        if (registeredUser != null) {
            restTemplate.delete(priceAlertUrl + "/api/users/" + registeredUser.id());
        }
        fetchedUser = null;
        registeredUser = null;
        createdAlert = null;
    }

    // ── Given ─────────────────────────────────────────────────────────────────

    /**
     * Step 1: call user-service to get the user's name and email.
     *
     * Feature line:  Given I fetch user with id 1 from the user-service
     * Maps to:       GET http://localhost:8081/api/users/1
     */
    @Given("I fetch user with id {long} from the user-service")
    public void iFetchUserFromUserService(long userId) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                userServiceUrl + "/api/users/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        fetchedUser = response.getBody();

        assertThat(fetchedUser).isNotNull();
        assertThat(fetchedUser).containsKeys("email", "name");
    }

    /**
     * Step 2: register the fetched user into the price-alert service.
     *
     * Feature line:  And I register that user in the price-alert service
     * Maps to:       POST http://localhost:9095/api/users
     *
     * Why? The price-alert service manages its own users table.
     * The user from user-service doesn't exist there yet — we register them
     * using their email + name so price-alert can link alerts to them.
     */
    @And("I register that user in the price-alert service")
    public void iRegisterThatUserInPriceAlertService() {
        String email = (String) fetchedUser.get("email");
        String name  = (String) fetchedUser.get("name");

        UserRequest request = new UserRequest(email, name);

        try {
            ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                    priceAlertUrl + "/api/users",
                    request,
                    UserResponse.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            registeredUser = response.getBody();
        } catch (HttpClientErrorException.Conflict e) {
            // User already exists from a previous test run — fetch by email
            registeredUser = restTemplate.getForObject(
                    priceAlertUrl + "/api/users/email/" + email,
                    UserResponse.class
            );
        }

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.id()).isNotNull();
    }

    // ── When ──────────────────────────────────────────────────────────────────

    /**
     * Step 3: create a PRICE_ABOVE alert using the price-alert userId.
     *
     * Feature line:  When I create a PRICE_ABOVE alert for symbol "AAPL" at threshold 200.0
     * Maps to:       POST http://localhost:9095/api/alerts
     */
    @When("I create a PRICE_ABOVE alert for symbol {string} at threshold {double}")
    public void iCreatePriceAboveAlert(String symbol, double threshold) {
        AlertRequest request = new AlertRequest(
                registeredUser.id(), symbol, "PRICE_ABOVE", threshold
        );

        ResponseEntity<AlertResponse> response = restTemplate.postForEntity(
                priceAlertUrl + "/api/alerts",
                request,
                AlertResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        createdAlert = response.getBody();
    }

    @When("I create a PRICE_BELOW alert for symbol {string} at threshold {double}")
    public void iCreatePriceBelowAlert(String symbol, double threshold) {
        AlertRequest request = new AlertRequest(
                registeredUser.id(), symbol, "PRICE_BELOW", threshold
        );

        ResponseEntity<AlertResponse> response = restTemplate.postForEntity(
                priceAlertUrl + "/api/alerts",
                request,
                AlertResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        createdAlert = response.getBody();
    }

    // ── Then / And ────────────────────────────────────────────────────────────

    @Then("the alert should be created successfully")
    public void theAlertShouldBeCreatedSuccessfully() {
        assertThat(createdAlert).isNotNull();
        assertThat(createdAlert.id()).isNotNull();
    }

    @And("the alert status should be {string}")
    public void theAlertStatusShouldBe(String expectedStatus) {
        assertThat(createdAlert.status()).isEqualTo(expectedStatus);
    }

    @And("the alert should be linked to the registered user")
    public void theAlertShouldBeLinkedToTheRegisteredUser() {
        assertThat(createdAlert.userId()).isEqualTo(registeredUser.id());
    }
}