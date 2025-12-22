# Price Alert System

A real-time price alert microservice built with Spring Boot 3.5, designed following **SOLID design principles** and industry best practices.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [SOLID Design Principles](#solid-design-principles)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Setup & Running](#setup--running)
- [Testing](#testing)
- [Load Testing](#load-testing)

---

## Overview

The Price Alert System allows users to create price alerts for financial securities. When market prices cross user-defined thresholds, notifications are triggered automatically.

### Key Features

- **Multiple Alert Types**: PRICE_ABOVE, PRICE_BELOW, PRICE_EQUALS, PRICE_BETWEEN
- **Real-time Processing**: Kafka consumer for price updates
- **WebSocket Support**: Live market feed integration
- **Optimized Matching**: O(log N + K) complexity using TreeMap indexing
- **Configurable Notifications**: Email, SMS, Push, Webhook channels
- **Custom Exception Handling**: Standardized error responses

### Simulated Stocks

| Symbol | Base Price |
|--------|------------|
| TCS | 3500.0 |
| INFY | 1450.0 |
| RELIANCE | 2400.0 |
| HDFC | 1600.0 |
| ICICIBANK | 950.0 |

---

## Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│              (Controllers + DTOs + Exception Handler)    │
├─────────────────────────────────────────────────────────┤
│                     Service Layer                        │
│    (Business Logic + Interfaces + Strategy Pattern)     │
├─────────────────────────────────────────────────────────┤
│                   Data Access Layer                      │
│              (Spring Data JPA Repositories)              │
├─────────────────────────────────────────────────────────┤
│                    Infrastructure                        │
│           (Kafka + WebSocket + PostgreSQL)               │
└─────────────────────────────────────────────────────────┘
```

### Data Flow

```
Price Update Sources                    Alert Processing
─────────────────────                   ─────────────────

  Kafka Topic          ──────►  PriceAlertConsumer
  (price_updates)                      │
                                       ▼
  WebSocket Feed       ──────►  MarketFeedWebSocketHandler
  (partner-service)                    │
                                       ▼
                              AlertMatchingService
                                       │
                          ┌────────────┴────────────┐
                          ▼                         ▼
                  AlertCacheService          NotificationService
                  (TreeMap Index)            (Email/SMS/Push)
```

---

## SOLID Design Principles

This codebase follows SOLID principles rigorously:

### S - Single Responsibility Principle

Each class has one reason to change:

| Class | Responsibility |
|-------|---------------|
| `UserService` | User CRUD operations |
| `AlertMatchingService` | Price matching logic only |
| `AlertCacheService` | Cache management only |
| `EmailService` | Email notifications only |
| `GlobalExceptionHandler` | Exception handling only |

### O - Open/Closed Principle

**Strategy Pattern** for alert type evaluation - adding new alert types requires NO modification to existing code:

```java
public interface AlertTriggerStrategy {
    boolean shouldTrigger(PriceAlert alert, double currentPrice);
    String getActionText();
    String getExplanation(PriceAlert alert, double currentPrice);
}

// Implementations:
├── PriceAboveStrategy      // PRICE_ABOVE logic
├── PriceBelowStrategy      // PRICE_BELOW logic
├── PriceEqualsStrategy     // PRICE_EQUALS logic (0.1% tolerance)
└── PriceBetweenStrategy    // PRICE_BETWEEN range logic
```

### L - Liskov Substitution Principle

All interface implementations are substitutable:
- `EmailService` implements `NotificationService`
- `AlertCacheService` implements `AlertCacheManager`
- `AlertMatchingService` implements `PriceProcessor`

### I - Interface Segregation Principle

Focused interfaces instead of fat ones:

```java
// Price processing only
public interface PriceProcessor {
    void processPrice(String symbol, double price);
}

// Cache management only
public interface AlertCacheManager {
    void invalidateCacheForSymbol(String symbol);
    void invalidateAllCache();
    Map<String, Object> getCacheStats();
}

// Notification only
public interface NotificationService {
    void sendPriceAlertNotification(PriceAlert alert, double currentPrice);
    void send(String to, String subject, String body);
}

// Alert evaluation only
public interface AlertEvaluationService {
    void evaluate(PriceUpdate priceUpdate);
}
```

### D - Dependency Inversion Principle

High-level modules depend on abstractions:

```java
// Controllers depend on interfaces, not implementations
public class AlertController {
    private final AlertCacheManager alertCacheManager;  // Interface
}

// Services depend on interfaces
public class AlertMatchingService implements PriceProcessor {
    private final NotificationService notificationService;  // Interface
    private final AlertCacheService alertCacheService;
}
```

---

## Project Structure

```
src/main/java/com/ankur/price_alert/
│
├── PriceAlertApplication.java          # Spring Boot entry point
│
├── controller/
│   ├── UserController.java             # User REST endpoints
│   ├── AlertController.java            # Alert REST endpoints
│   └── SimulatorController.java        # Price simulation endpoints
│
├── service/
│   ├── NotificationService.java        # Interface
│   ├── AlertEvaluationService.java     # Interface
│   ├── PriceProcessor.java             # Interface
│   ├── AlertCacheManager.java          # Interface
│   ├── UserService.java                # User business logic
│   ├── PriceAlertService.java          # Alert evaluation
│   ├── AlertMatchingService.java       # Price matching (O(log N + K))
│   ├── AlertCacheService.java          # Cache management
│   ├── EmailService.java               # Email notifications
│   ├── PriceAlertConsumer.java         # Kafka consumer
│   ├── PriceFeedSimulator.java         # Test data generator
│   ├── MarketFeedService.java          # WebSocket client
│   └── MarketFeedWebSocketHandler.java # WebSocket handler
│
├── strategy/
│   ├── AlertTriggerStrategy.java       # Strategy interface
│   ├── PriceAboveStrategy.java         # PRICE_ABOVE
│   ├── PriceBelowStrategy.java         # PRICE_BELOW
│   ├── PriceEqualsStrategy.java        # PRICE_EQUALS
│   ├── PriceBetweenStrategy.java       # PRICE_BETWEEN
│   └── AlertStrategyFactory.java       # Strategy factory
│
├── exception/
│   ├── PriceAlertException.java        # Base exception
│   ├── ResourceNotFoundException.java  # Base for 404 errors
│   ├── UserNotFoundException.java      # User not found (404)
│   ├── AlertNotFoundException.java     # Alert not found (404)
│   ├── AlertQuotaExceededException.java# Quota exceeded (400)
│   ├── DuplicateResourceException.java # Duplicate resource (409)
│   ├── InvalidAlertConfigurationException.java # Invalid config (400)
│   └── GlobalExceptionHandler.java     # Centralized exception handling
│
├── model/
│   ├── User.java                       # User entity
│   ├── PriceAlert.java                 # Alert entity
│   ├── AlertType.java                  # PRICE_ABOVE, PRICE_BELOW, etc.
│   ├── AlertStatus.java                # ACTIVE, TRIGGERED, PAUSED, EXPIRED
│   ├── NotificationChannel.java        # EMAIL, SMS, PUSH, WEBHOOK
│   └── PriceUpdate.java                # Price update POJO
│
├── dto/
│   ├── UserRequest.java / UserResponse.java
│   ├── AlertRequest.java / AlertResponse.java
│   ├── ErrorResponse.java              # Standardized error response
│   └── QuoteData.java / QuoteFeedMessage.java
│
└── repository/
    ├── UserRepository.java
    └── PriceAlertRepository.java
```

---

## API Reference

### User Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create user |
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/users/email/{email}` | Get user by email |
| GET | `/api/users` | List all users |
| PUT | `/api/users/{id}` | Update user |
| PATCH | `/api/users/{id}/notification-channel` | Update notification preference |
| PATCH | `/api/users/{id}/activate` | Activate user |
| PATCH | `/api/users/{id}/deactivate` | Deactivate user |
| DELETE | `/api/users/{id}` | Delete user |

### Alert Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/alerts` | Create alert |
| GET | `/api/alerts/{id}` | Get alert by ID |
| GET | `/api/alerts/user/{userId}` | Get alerts by user |
| GET | `/api/alerts/user/{userId}/active` | Get active alerts by user |
| GET | `/api/alerts/symbol/{symbol}` | Get alerts by symbol |
| PATCH | `/api/alerts/{id}/activate` | Activate alert |
| PATCH | `/api/alerts/{id}/deactivate` | Deactivate alert |
| DELETE | `/api/alerts/{id}` | Delete alert |
| GET | `/api/alerts/cache/stats` | Get cache statistics |
| POST | `/api/alerts/cache/invalidate` | Invalidate all cache |

### Simulator Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/simulator/tick` | Send single price update |
| POST | `/api/simulator/burst?count=N` | Send N rapid updates |
| POST | `/api/simulator/loadtest?rate=X&duration=Y` | Load test at X msg/sec for Y seconds |
| POST | `/api/simulator/start` | Start continuous feed (1 msg/sec) |
| POST | `/api/simulator/stop` | Stop continuous feed |
| GET | `/api/simulator/prices` | Get current simulated prices |

### Error Response Format

All errors return a standardized JSON response:

```json
{
  "errorCode": "USER_NOT_FOUND",
  "message": "User not found with id: 123",
  "path": "/api/users/123",
  "timestamp": "2024-01-15T10:30:00",
  "details": {
    "resourceType": "User",
    "resourceId": 123
  }
}
```

### Exception Types

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| `UserNotFoundException` | 404 | USER_NOT_FOUND |
| `AlertNotFoundException` | 404 | ALERT_NOT_FOUND |
| `AlertQuotaExceededException` | 400 | ALERT_QUOTA_EXCEEDED |
| `DuplicateResourceException` | 409 | DUPLICATE_RESOURCE |
| `InvalidAlertConfigurationException` | 400 | INVALID_ALERT_CONFIG |

---

## Setup & Running

### Prerequisites

- Java 21
- Kafka (Homebrew: `brew install kafka`)
- PostgreSQL (via Docker)
- Docker & Docker Compose

### 1. Start Infrastructure

```bash
# Start PostgreSQL and Conduktor Console
docker-compose up -d

# Start Kafka (Homebrew)
brew services start kafka
```

### 2. Configure Kafka for Docker Access

Add to `/etc/hosts` (one-time setup):
```bash
echo "127.0.0.1 host.docker.internal" | sudo tee -a /etc/hosts
```

Update Kafka config (`/opt/homebrew/etc/kafka/server.properties`):
```properties
advertised.listeners=PLAINTEXT://host.docker.internal:9092,CONTROLLER://localhost:9093
```

Restart Kafka:
```bash
brew services restart kafka
```

### 3. Configuration

Application properties (`src/main/resources/application.properties`):

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/price_alerts
spring.datasource.username=postgres
spring.datasource.password=postgres123

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=alert-service

# Cache Settings
alert.cache.ttl.minutes=5
alert.trigger.cooldown.seconds=60

# WebSocket Market Feed
market.feed.websocket.url=ws://localhost:9090/quotes
market.feed.enabled=true
```

### 4. Run the Application

```bash
./gradlew bootRun
```

### Service Ports

| Service | Port |
|---------|------|
| Spring Boot App | 8080 |
| Kafka | 9092 |
| PostgreSQL | 5432 |
| Conduktor Console | 9080 |

### Monitoring - Conduktor Console

Access Kafka UI at: http://localhost:9080

- **Email:** `admin@admin.io`
- **Password:** `Admin123!`

---

## Testing

### Run All Tests

```bash
./gradlew test
```

### Test Classes

| Test Class | Coverage |
|------------|----------|
| `PriceAlertServiceTest` | Alert evaluation logic |
| `AlertMatchingServiceTest` | Price matching with TreeMap |
| `AlertCacheServiceTest` | Cache operations |

### Sample Test

```java
@Test
void processPrice_PriceAboveThreshold_ShouldTriggerAlert() {
    // Given
    PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 400.0);
    when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

    // When - price 450 > threshold 400
    alertMatchingService.processPrice("TCS", 450.0);

    // Then
    verify(notificationService).sendPriceAlertNotification(eq(alert), eq(450.0));
    assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
}
```

---

## Load Testing

### Install hey

```bash
brew install hey
```

### Run Load Tests

```bash
# Quick test (100 requests)
hey -n 100 -c 10 -m POST http://localhost:8080/api/simulator/tick

# Medium load (1000 requests, 50 concurrent)
hey -n 1000 -c 50 -m POST http://localhost:8080/api/simulator/tick

# Heavy load (10000 requests, 100 concurrent)
hey -n 10000 -c 100 -m POST http://localhost:8080/api/simulator/tick

# Sustained load for 30 seconds
hey -z 30s -c 50 -m POST http://localhost:8080/api/simulator/tick
```

---

## Performance Optimizations

### TreeMap-Based Alert Matching

```
Time Complexity:
├── Previous: O(N) linear scan per price tick
└── Current:  O(log N + K) where K = triggered alerts

Space Complexity: O(N) for cached alerts
```

### Caching Strategy

- **Symbol-based cache**: Alerts indexed by symbol
- **TTL**: Configurable expiration (default 5 minutes)
- **Lazy loading**: Cache populated on first access
- **Invalidation**: Automatic on alert create/update/delete

### Cooldown Mechanism

- Prevents duplicate notifications
- Configurable cooldown period (default 60 seconds)
- Per-alert tracking

---

## Design Patterns Used

| Pattern | Implementation |
|---------|---------------|
| **Strategy** | Alert type evaluation (`AlertTriggerStrategy`) |
| **Factory** | Strategy lookup (`AlertStrategyFactory`) |
| **Repository** | Data access (`JpaRepository`) |
| **DTO** | Request/Response mapping |
| **Template Method** | WebSocket handling (`TextWebSocketHandler`) |
| **Observer** | Kafka message consumption (`@KafkaListener`) |

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.5.6 |
| Language | Java 21 |
| Database | PostgreSQL |
| Messaging | Apache Kafka |
| ORM | Spring Data JPA / Hibernate |
| Build | Gradle |
| Testing | JUnit 5 + Mockito |
| Code Gen | Lombok |

---

## License

MIT License