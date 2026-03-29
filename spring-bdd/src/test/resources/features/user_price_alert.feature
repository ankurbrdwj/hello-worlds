Feature: Cross-service price alert creation
  As a user registered in the user-service
  I want to create a price alert in the price-alert service
  So that I am notified when a stock hits my target price

  Scenario: Fetch user from user-service and create a PRICE_ABOVE alert
    Given I fetch user with id 1 from the user-service
    And I register that user in the price-alert service
    When I create a PRICE_ABOVE alert for symbol "AAPL" at threshold 200.0
    Then the alert should be created successfully
    And the alert status should be "ACTIVE"
    And the alert should be linked to the registered user

  Scenario: Fetch user from user-service and create a PRICE_BELOW alert
    Given I fetch user with id 2 from the user-service
    And I register that user in the price-alert service
    When I create a PRICE_BELOW alert for symbol "TSLA" at threshold 150.0
    Then the alert should be created successfully
    And the alert status should be "ACTIVE"
    And the alert should be linked to the registered user