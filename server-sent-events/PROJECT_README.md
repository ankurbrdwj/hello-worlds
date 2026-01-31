# Server-Sent Events (SSE) Notification Service

A production-ready, real-time notification system built with Spring Boot, demonstrating Server-Sent Events (SSE) for push notifications, with Kafka for event streaming and PostgreSQL for event persistence.

## Architecture Overview

This application implements the notification pattern described in the Pliant blog post about building real-time, reliable notifications with Server-Sent Events.

### Key Components

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Client    │  SSE    │   Spring    │  Kafka  │   Kafka     │
│  (Browser)  │◄────────┤    Boot     │◄────────┤   Topics    │
│             │         │   Server    │         │             │
└─────────────┘         └─────────────┘         └─────────────┘
                              │
                              │ JPA
                              ▼
                        ┌─────────────┐
                        │ PostgreSQL  │
                        │  (Events)   │
                        └─────────────┘
```

### Features

1. **Server-Sent Events (SSE)**
   - Unidirectional server-to-client push notifications
   - Automatic reconnection support
   - Multiple concurrent connections per user
   - 30-minute connection timeout

2. **Event Persistence**
   - All notification events stored in PostgreSQL
   - Persistent events delivered on reconnection
   - Automatic cleanup of expired events
   - Delivery tracking and auditing

3. **Kafka Integration**
   - Event-driven architecture
   - Scalable across multiple service instances
   - Asynchronous event processing
   - Fan-out pattern for multi-device delivery

4. **Connection Management**
   - In-memory connection registry
   - Graceful handling of timeouts and errors
   - Pending event delivery on reconnection
   - Multiple tabs/devices support

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.9**
- **PostgreSQL 16** (event persistence)
- **Apache Kafka 7.6.0** (event streaming)
- **Lombok** (boilerplate reduction)
- **Spring Data JPA** (database access)
- **Spring Kafka** (Kafka integration)

## Project Structure

```
src/
├── main/
│   ├── java/com/ankur/sse/
│   │   ├── config/              # Configuration classes
│   │   │   ├── KafkaConfig.java
│   │   │   └── SchedulingConfig.java
│   │   ├── controller/          # REST controllers
│   │   │   ├── NotificationController.java
│   │   │   └── SseController.java
│   │   ├── dto/                 # Data transfer objects
│   │   │   └── NotificationEvent.java
│   │   ├── kafka/               # Kafka producers/consumers
│   │   │   ├── NotificationEventConsumer.java
│   │   │   └── NotificationEventProducer.java
│   │   ├── model/               # JPA entities
│   │   │   ├── EventType.java
│   │   │   └── SseRequest.java
│   │   ├── repository/          # Data repositories
│   │   │   └── SseRequestRepository.java
│   │   └── service/             # Business logic
│   │       └── SseNotificationService.java
│   └── resources/
│       ├── application.yml      # Application configuration
│       └── static/
│           └── index.html       # Web client UI
└── test/
```

## Getting Started

### Prerequisites

- Java 21 or higher
- Docker & Docker Compose

### Running the Application

1. **Start Infrastructure Services** (PostgreSQL & Kafka)
   ```bash
   docker-compose up -d
   ```

2. **Build the Application**
   ```bash
   ./gradlew build
   ```

3. **Run the Spring Boot Application**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the Web Client**
   Open your browser and navigate to:
   ```
   http://localhost:8080
   ```

### Testing the System

#### Method 1: Using the Web UI

1. Open `http://localhost:8080` in multiple browser tabs
2. In the first tab:
   - Enter User ID: `user123`
   - Click "Connect to SSE"
3. In the second tab:
   - Set Target User ID: `user123`
   - Enter a message
   - Click "Send Notification"
4. Watch the notification appear in the first tab in real-time!

#### Method 2: Using cURL

**Connect to SSE (in terminal):**
```bash
curl -N http://localhost:8080/api/sse/connect?userId=user123
```

**Send a notification (in another terminal):**
```bash
curl -X POST http://localhost:8080/api/notifications/quick \
  -d "userId=user123&message=Hello from cURL!"
```

**Send a biometric auth request:**
```bash
curl -X POST http://localhost:8080/api/notifications/biometric-auth/user123
```

**Send a custom notification:**
```bash
curl -X POST http://localhost:8080/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "type": "NOTIFICATION",
    "payload": "Custom notification message",
    "persistent": true
  }'
```

## API Endpoints

### SSE Endpoints

- **GET** `/api/sse/connect?userId={userId}`
  - Establish SSE connection for a user
  - Returns: `text/event-stream`

- **GET** `/api/sse/connections/{userId}`
  - Get active connection count for a user
  - Returns: `integer`

### Notification Endpoints

- **POST** `/api/notifications/send`
  - Send a notification event via Kafka
  - Body: `NotificationEvent` JSON
  - Returns: Status message

- **POST** `/api/notifications/quick?userId={userId}&message={message}`
  - Quick notification with default settings
  - Returns: Status message

- **POST** `/api/notifications/biometric-auth/{userId}`
  - Trigger a biometric authentication request
  - Returns: Status message

## Event Types

The system supports the following event types:

- `NOTIFICATION` - General notifications
- `BIOMETRIC_AUTH_REQUEST` - Biometric authentication requests
- `TRANSACTION_ALERT` - Transaction-related alerts
- `SYSTEM_MESSAGE` - System-level messages
- `CUSTOM_EVENT` - Custom events

## How It Works

### 1. SSE Connection Flow

```
Client                          Server                      Database
  │                               │                            │
  ├──── GET /api/sse/connect ────►│                            │
  │                               ├──── Register connection    │
  │                               ├──── Send ping event        │
  │◄─── event: ping ──────────────┤                            │
  │                               ├──── Query pending events ─►│
  │                               ◄──── Pending events ────────┤
  │◄─── event: NOTIFICATION ──────┤                            │
  │                               │                            │
```

### 2. Event Delivery Flow

```
Producer                    Kafka                   Consumer                  Client
  │                          │                        │                         │
  ├─ Publish event ─────────►│                        │                         │
  │                          ├─ Store in topic ───────►│                         │
  │                          │                        ├─ Process event           │
  │                          │                        ├─ Save to DB              │
  │                          │                        ├─ Deliver via SSE ───────►│
  │                          │                        │                         │
```

### 3. Reconnection Flow

When a client reconnects:
1. Server validates the user and registers new connection
2. Sends initial ping event
3. Queries database for undelivered persistent events
4. Delivers all pending events in order
5. Marks events as delivered

## Database Schema

### sse_requests Table

| Column       | Type         | Description                          |
|--------------|--------------|--------------------------------------|
| id           | BIGINT       | Primary key (auto-increment)         |
| user_id      | VARCHAR      | Target user identifier               |
| type         | VARCHAR      | Event type (enum)                    |
| payload      | TEXT         | Event payload/message                |
| created_at   | TIMESTAMP    | Event creation time                  |
| expires_at   | TIMESTAMP    | Event expiration time                |
| persistent   | BOOLEAN      | Whether to retry on reconnection     |
| delivered    | BOOLEAN      | Delivery status                      |
| delivered_at | TIMESTAMP    | Actual delivery time                 |

## Configuration

### Application Configuration (application.yml)

Key configuration parameters:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/notifications
    username: postgres
    password: postgres

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-service-group

kafka:
  topic:
    notifications: notification-events

server:
  port: 8080
```

### Docker Compose Services

- **PostgreSQL**: Port 5432
- **Zookeeper**: Port 2181
- **Kafka**: Ports 9092 (external), 29092 (internal)

## Key Design Patterns

### 1. Event Persistence
All notification events are persisted to PostgreSQL before delivery, ensuring reliability and enabling:
- Retry on connection failure
- Multi-device delivery
- Event auditing and compliance
- Reconnection handling

### 2. Connection Registry
In-memory map maintains active SSE connections:
- `Map<String, List<SseEmitter>>` per user
- Thread-safe with `ConcurrentHashMap` and `CopyOnWriteArrayList`
- Automatic cleanup on timeout/error

### 3. Graceful Failure Handling
- Connection timeouts trigger cleanup
- Failed delivery keeps event in database
- Automatic reconnection attempts
- No data loss on temporary disconnections

### 4. Scheduled Cleanup
- Hourly job removes expired events
- Prevents database bloat
- Configurable retention policy

## Production Considerations

### Scalability
- Kafka enables horizontal scaling across multiple instances
- Each instance maintains its own connection registry
- Fan-out pattern ensures all instances receive events
- Load balancer should use sticky sessions for SSE

### Monitoring
- Log connection lifecycle events
- Track delivery success/failure rates
- Monitor event queue depth
- Alert on excessive pending events

### Security
- Add authentication/authorization
- Validate user IDs and permissions
- Use HTTPS in production
- Implement rate limiting
- Add CORS configuration

### Performance
- Connection pool tuning for PostgreSQL
- Kafka partition strategy
- SSE timeout configuration
- Event retention policies

## Troubleshooting

### Issue: SSE connection drops immediately
- Check firewall/proxy settings
- Verify browser SSE support
- Check server logs for errors
- Increase timeout if needed

### Issue: Events not received
- Verify Kafka is running and accessible
- Check topic creation
- Verify user ID matches
- Check database for pending events

### Issue: Duplicate events
- Check Kafka consumer group configuration
- Verify idempotency handling
- Review event delivery logic

## Testing Multi-Device Scenarios

1. Open the web client in multiple browser tabs
2. Connect all tabs with the same user ID
3. Send a notification
4. Observe event delivery to all connected tabs simultaneously
5. Disconnect one tab
6. Send another notification
7. Reconnect the tab and observe pending event delivery

## License

This is a demonstration project for educational purposes.

## References

- [Pliant Blog: Building Real-Time Notifications with SSE](https://www.pliant.io/blog)
- [Server-Sent Events Specification](https://html.spec.whatwg.org/multipage/server-sent-events.html)
- [Spring Boot SSE Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html#mvc-ann-async-sse)