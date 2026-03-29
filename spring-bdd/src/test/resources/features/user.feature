Feature: User Management
  As a platform user
  I want to manage my account
  So that I can create price alerts

  Scenario: Create a new user successfully
    Given no user exists with email "alice@example.com"
    When I create a user with email "alice@example.com" and name "Alice"
    Then the user should be created successfully
    And the user's name should be "Alice"
    And the user should be active by default

  Scenario: Prevent duplicate email registration
    Given a user already exists with email "bob@example.com" and name "Bob"
    When I try to create another user with email "bob@example.com" and name "Bob Duplicate"
    Then the creation should fail with a duplicate email error

  Scenario: Find user by email
    Given a user already exists with email "carol@example.com" and name "Carol"
    When I look up the user by email "carol@example.com"
    Then the user should be found
    And the user's name should be "Carol"

  Scenario: Return not found for unknown email
    Given no user exists with email "ghost@example.com"
    When I look up the user by email "ghost@example.com"
    Then the user should not be found

  Scenario: Deactivate a user
    Given a user already exists with email "dave@example.com" and name "Dave"
    When I deactivate the user with email "dave@example.com"
    Then the user should be inactive

  Scenario: Reactivate a deactivated user
    Given a user already exists with email "eve@example.com" and name "Eve"
    And the user with email "eve@example.com" is deactivated
    When I activate the user with email "eve@example.com"
    Then the user should be active