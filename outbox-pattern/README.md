# Outbox Pattern Implementation

A complete demonstration of the Transactional Outbox Pattern using Spring Boot, PostgreSQL, and Docker.

---

## Table of Contents
- [Understanding the Outbox Pattern](#understanding-the-outbox-pattern)
- [The Problem: Dual Write](#the-problem-dual-write)
- [The Solution: Transactional Outbox](#the-solution-transactional-outbox)
- [How Our Implementation Works](#how-our-implementation-works)
- [Setup and Run](#setup-and-run)
- [Testing](#testing-the-outbox-pattern)

---

## Understanding the Outbox Pattern

### Definition

> **The Transactional Outbox Pattern** is a microservices design pattern that ensures reliable event publishing by storing events in an "outbox" table within the same database transaction as the business data, then having a separate process read and publish those events to a message broker.

### Core Principles

```
┌─────────────────────────────────────────────────────────────┐
│                 OUTBOX PATTERN PRINCIPLES                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Single Transaction for Business Data + Event           │
│     → Both succeed or both fail together (Atomicity)       │
│                                                             │
│  2. Event Stored in Database (not sent directly)           │
│     → Database becomes the source of truth                 │
│                                                             │
│  3. Separate Process Publishes Events                      │
│     → Decouples business logic from message delivery       │
│                                                             │
│  4. At-Least-Once Delivery Guarantee                       │
│     → Events never lost, may be retried                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## The Problem: Dual Write

### What is the Dual Write Problem?

When you need to **update a database AND publish a message** to notify other services, you face a consistency challenge:

```
╔═══════════════════════════════════════════════════════════════╗
║                    ❌ THE DUAL WRITE PROBLEM                  ║
╚═══════════════════════════════════════════════════════════════╝

Scenario: Creating an Order + Publishing OrderCreated Event

┌─────────────────────────────────────────────────────────────┐
│  Approach 1: Database First, Then Message Broker           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. ✅ Save Order to Database                               │
│  2. 💥 Application crashes before publishing message        │
│                                                             │
│  Result: Order exists, but NO EVENT published!             │
│          Other services never know about the order.        │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Approach 2: Message Broker First, Then Database           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. ✅ Publish message to broker                            │
│  2. 💥 Database save fails (constraint violation, crash)    │
│                                                             │
│  Result: Event published, but NO ORDER in database!        │
│          System is in inconsistent state.                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Approach 3: Distributed Transaction (2PC)                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ⚠️  Complex, slow, and often not supported by brokers     │
│  ⚠️  Reduces availability (CAP theorem)                     │
│  ⚠️  Requires XA-compatible components                      │
│                                                             │
│  Result: Rarely practical in modern microservices          │
└─────────────────────────────────────────────────────────────┘
```

### Why Can't We Just Use a Transaction?

```
┌──────────────────────────────────────────────────────────┐
│  Database and Message Broker are SEPARATE systems       │
│  with DIFFERENT transaction managers                     │
│                                                          │
│  ┌──────────────┐           ┌──────────────┐           │
│  │  PostgreSQL  │     ✗     │    Kafka     │           │
│  │ Transaction  │  No Link  │   Producer   │           │
│  │   Manager    │           │              │           │
│  └──────────────┘           └──────────────┘           │
│                                                          │
│  You cannot wrap both operations in a single ACID       │
│  transaction without expensive distributed protocols.   │
└──────────────────────────────────────────────────────────┘
```

---

## The Solution: Transactional Outbox

### How It Solves the Dual Write Problem

```
╔═══════════════════════════════════════════════════════════════╗
║              ✅ THE OUTBOX PATTERN SOLUTION                   ║
╚═══════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────┐
│  STEP 1: Single Database Transaction                        │
│  ═══════════════════════════════════════                    │
│                                                             │
│  BEGIN TRANSACTION                                          │
│    1. INSERT INTO orders (...)          ← Business Data    │
│    2. INSERT INTO outbox_events (...)   ← Event Record     │
│  COMMIT                                                     │
│                                                             │
│  ✅ Both succeed together OR both fail together            │
│  ✅ ACID guarantees ensure consistency                      │
│  ✅ No dual write problem!                                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  STEP 2: Background Process Publishes Events               │
│  ══════════════════════════════════════════                │
│                                                             │
│  Every N seconds:                                           │
│    1. SELECT * FROM outbox_events WHERE status='PENDING'   │
│    2. FOR EACH event:                                       │
│         - Publish to message broker (Kafka, RabbitMQ...)   │
│         - UPDATE outbox_events SET status='PROCESSED'      │
│                                                             │
│  ✅ Decoupled from business logic                           │
│  ✅ Can retry on failure                                    │
│  ✅ No data loss even if app crashes                        │
└─────────────────────────────────────────────────────────────┘
```

### Pattern Components

```
┌───────────────────────────────────────────────────────────────┐
│                                                               │
│   Application Service Layer                                  │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  OrderService.createOrder()                         │   │
│   │                                                      │   │
│   │  @Transactional                                     │   │
│   │  ┌────────────────────────────────────────────┐    │   │
│   │  │ 1. Save Order Entity                       │    │   │
│   │  │ 2. Save OutboxEvent Entity                 │    │   │
│   │  └────────────────────────────────────────────┘    │   │
│   └─────────────────────────────────────────────────────┘   │
│                           │                                   │
│                           ▼                                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │             Database (PostgreSQL)                   │   │
│   │  ┌──────────────┐      ┌──────────────────────┐    │   │
│   │  │   orders     │      │   outbox_events      │    │   │
│   │  │              │      │                      │    │   │
│   │  │ id: 1        │      │ id: 1                │    │   │
│   │  │ customer: X  │      │ event: OrderCreated  │    │   │
│   │  │ product: Y   │      │ status: PENDING      │    │   │
│   │  └──────────────┘      └──────────────────────┘    │   │
│   └─────────────────────────────────────────────────────┘   │
│                           │                                   │
│                           │ Polls for PENDING                 │
│                           ▼                                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  OutboxProcessor (Scheduled Job)                    │   │
│   │                                                      │   │
│   │  @Scheduled(fixedDelay = 5000)                      │   │
│   │  ┌────────────────────────────────────────────┐    │   │
│   │  │ 1. Poll pending events                     │    │   │
│   │  │ 2. Publish to Kafka/RabbitMQ               │    │   │
│   │  │ 3. Mark as PROCESSED                       │    │   │
│   │  └────────────────────────────────────────────┘    │   │
│   └─────────────────────────────────────────────────────┘   │
│                           │                                   │
│                           ▼                                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │      Message Broker (Kafka, RabbitMQ, etc.)         │   │
│   │                                                      │   │
│   │         [OrderCreated Event Published]              │   │
│   │                                                      │   │
│   │    ↓                ↓                 ↓              │   │
│   │  Service A      Service B        Service C          │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

---

## How Our Implementation Works

### Pattern Requirements vs Our Implementation

```
┌─────────────────────────────────────────────────────────────────┐
│  ✅ REQUIREMENT 1: Transactional Consistency                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Pattern Says: "Store events in same transaction as data"      │
│                                                                 │
│  Our Implementation:                                            │
│  📁 OrderService.java:22-46                                     │
│                                                                 │
│     @Transactional                                             │
│     public Order createOrder(Order order) {                    │
│         Order savedOrder = orderRepository.save(order);        │
│         OutboxEvent event = new OutboxEvent(...);              │
│         outboxEventRepository.save(event);  // Same TX!       │
│         return savedOrder;                                     │
│     }                                                           │
│                                                                 │
│  ✅ Both writes happen in ONE database transaction             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ✅ REQUIREMENT 2: Persistent Outbox Table                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Pattern Says: "Events stored in a database table"             │
│                                                                 │
│  Our Implementation:                                            │
│  📁 schema.sql:14-24                                            │
│                                                                 │
│     CREATE TABLE outbox_events (                               │
│         id BIGSERIAL PRIMARY KEY,                              │
│         aggregate_type VARCHAR(255),    ← "Order"             │
│         aggregate_id BIGINT,            ← Order ID            │
│         event_type VARCHAR(255),        ← "OrderCreated"      │
│         payload TEXT,                   ← Full event JSON     │
│         status VARCHAR(50),             ← PENDING/PROCESSED   │
│         created_at TIMESTAMP,                                  │
│         processed_at TIMESTAMP                                 │
│     );                                                          │
│                                                                 │
│  ✅ Dedicated table for outbox events with status tracking     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ✅ REQUIREMENT 3: Separate Publisher Process                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Pattern Says: "Background process publishes events"           │
│                                                                 │
│  Our Implementation:                                            │
│  📁 OutboxProcessor.java:24-56                                  │
│                                                                 │
│     @Scheduled(fixedDelay = 5000)  // Every 5 seconds         │
│     @Transactional                                             │
│     public void processOutboxEvents() {                        │
│         List<OutboxEvent> events =                             │
│             outboxEventRepository.findPendingEvents(10);       │
│                                                                 │
│         for (OutboxEvent event : events) {                     │
│             publishEvent(event);     // To message broker     │
│             event.markAsProcessed();                           │
│             outboxEventRepository.save(event);                 │
│         }                                                       │
│     }                                                           │
│                                                                 │
│  ✅ Independent scheduled job polls and publishes events       │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ✅ REQUIREMENT 4: Idempotency & Retry Support                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Pattern Says: "Handle failures gracefully, retry failed msgs" │
│                                                                 │
│  Our Implementation:                                            │
│  📁 OutboxEvent.java:34-41                                      │
│                                                                 │
│     public void markAsProcessed() {                            │
│         this.status = "PROCESSED";                             │
│         this.processedAt = LocalDateTime.now();                │
│     }                                                           │
│                                                                 │
│     public void markAsFailed() {                               │
│         this.status = "FAILED";                                │
│     }                                                           │
│                                                                 │
│  📁 OutboxProcessor.java:41-47                                  │
│                                                                 │
│     try {                                                       │
│         publishEvent(event);                                   │
│         event.markAsProcessed();                               │
│     } catch (Exception e) {                                    │
│         event.markAsFailed();  // Can be retried later        │
│     }                                                           │
│                                                                 │
│  ✅ Status tracking allows retry of failed events              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ✅ REQUIREMENT 5: Event Ordering & Audit Trail                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Pattern Says: "Events processed in order, audit history"      │
│                                                                 │
│  Our Implementation:                                            │
│  📁 OutboxEventRepository.java:13-14                            │
│                                                                 │
│     @Query("SELECT * FROM outbox_events                        │
│            WHERE status = 'PENDING'                            │
│            ORDER BY created_at ASC                             │
│            LIMIT :limit")                                      │
│     List<OutboxEvent> findPendingEvents(int limit);            │
│                                                                 │
│  📁 schema.sql:27                                               │
│                                                                 │
│     CREATE INDEX idx_outbox_status                             │
│         ON outbox_events(status, created_at);                  │
│                                                                 │
│  ✅ Events processed in chronological order                    │
│  ✅ Full audit trail with timestamps                           │
└─────────────────────────────────────────────────────────────────┘
```

### Complete Flow Diagram

```
╔═══════════════════════════════════════════════════════════════════╗
║                    COMPLETE EXECUTION FLOW                        ║
╚═══════════════════════════════════════════════════════════════════╝

   Client                Application              Database
     │                       │                        │
     │  POST /api/orders     │                        │
     ├──────────────────────>│                        │
     │                       │                        │
     │                       │  BEGIN TRANSACTION     │
     │                       ├───────────────────────>│
     │                       │                        │
     │                       │  INSERT INTO orders    │
     │                       ├───────────────────────>│
     │                       │                        │
     │                       │  <Order Saved: ID=1>   │
     │                       │<───────────────────────┤
     │                       │                        │
     │                       │  INSERT INTO           │
     │                       │  outbox_events         │
     │                       │  (status='PENDING')    │
     │                       ├───────────────────────>│
     │                       │                        │
     │                       │  <Event Saved: ID=1>   │
     │                       │<───────────────────────┤
     │                       │                        │
     │                       │  COMMIT                │
     │                       ├───────────────────────>│
     │                       │                        │
     │  <201 Created>        │                        │
     │<──────────────────────┤                        │
     │                       │                        │
     │                       │                        │
     ╞═══════════════════════╪════════════════════════╪════════════╡
     │   (5 seconds later)   │                        │
     ╞═══════════════════════╪════════════════════════╪════════════╡
     │                       │                        │
                    ┌────────┴────────┐               │
                    │ OutboxProcessor │               │
                    │  @Scheduled     │               │
                    └────────┬────────┘               │
                             │                        │
                             │  SELECT * FROM         │
                             │  outbox_events WHERE   │
                             │  status='PENDING'      │
                             ├───────────────────────>│
                             │                        │
                             │  <Returns Event ID=1>  │
                             │<───────────────────────┤
                             │                        │
                             │                        │
                    ┌────────▼────────────────┐       │
                    │ Publish to Kafka/       │       │
                    │ RabbitMQ/SNS            │       │
                    │ (Simulated in our code) │       │
                    └────────┬────────────────┘       │
                             │                        │
                             │  UPDATE outbox_events  │
                             │  SET status='PROCESSED'│
                             │  WHERE id=1            │
                             ├───────────────────────>│
                             │                        │
                             │  <Update Success>      │
                             │<───────────────────────┤
                             │                        │
                             └────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  KEY BENEFITS DEMONSTRATED:                                     │
│                                                                 │
│  ✅ If app crashes after COMMIT → Event still in DB, will be   │
│     published when app restarts                                │
│                                                                 │
│  ✅ If publish fails → Event stays PENDING, will retry next    │
│     scheduled run                                              │
│                                                                 │
│  ✅ No data loss, guaranteed delivery, eventual consistency    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

- Docker and Docker Compose
- Java 21
- Gradle (included via wrapper)

---

## Setup and Run

### 1. Start PostgreSQL

```bash
docker-compose up -d
```

This will start PostgreSQL on port 5434 with:
- Database: `outboxdb`
- Username: `outboxuser`
- Password: `outboxpass`

The schema will be automatically initialized with `orders` and `outbox_events` tables.

### 2. Build the Application

```bash
./gradlew build
```

### 3. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

---

## Testing the Outbox Pattern

### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "product": "Laptop",
    "quantity": 1,
    "totalPrice": 999.99
  }'
```

### What Happens:

```
┌──────────────────────────────────────────────────────────────┐
│  1. ✅ Order saved to orders table                           │
│  2. ✅ Outbox event created (same transaction)               │
│  3. ⏰ Wait 5 seconds...                                     │
│  4. ✅ OutboxProcessor polls for PENDING events              │
│  5. ✅ Event published to message broker (simulated)         │
│  6. ✅ Event marked as PROCESSED                             │
└──────────────────────────────────────────────────────────────┘
```

### Verify in Database

```bash
# Connect to PostgreSQL
docker exec -it outbox-postgres psql -U outboxuser -d outboxdb

# Check orders
SELECT * FROM orders;

# Check outbox events
SELECT id, aggregate_type, event_type, status, created_at, processed_at FROM outbox_events;
```

### Check Logs

Watch the application logs to see:
- Order creation
- Outbox event creation
- Scheduled processing of outbox events
- Event publishing (simulated)

```
Creating order for customer: John Doe
Order saved with ID: 1
Outbox event created for order ID: 1
Processing 1 pending outbox events
Publishing event to message broker: Event ID=1, Type=OrderCreated, Aggregate=Order:1
Successfully processed event ID: 1 for Order with event type: OrderCreated
```

---

## Project Structure

```
src/main/java/com/ankur/outbox/
├── OutboxPatternApplication.java    # Main application class
├── controller/
│   └── OrderController.java         # REST API endpoints
├── dto/
│   └── CreateOrderRequest.java      # Request DTO
├── model/
│   ├── Order.java                   # Order entity
│   └── OutboxEvent.java             # Outbox event entity
├── repository/
│   ├── OrderRepository.java         # Order repository
│   └── OutboxEventRepository.java   # Outbox repository
├── scheduler/
│   └── OutboxProcessor.java         # Scheduled outbox processor
└── service/
    └── OrderService.java            # Business logic with outbox
```

---

## Key Components

### Order Entity
Represents a customer order with fields like customer name, product, quantity, and price.

### OutboxEvent Entity
Stores events to be published with:
- `aggregateType`: Type of entity (e.g., "Order")
- `aggregateId`: ID of the entity
- `eventType`: Type of event (e.g., "OrderCreated")
- `payload`: JSON representation of the event
- `status`: PENDING, PROCESSED, or FAILED

### OrderService
Handles order creation with transactional outbox:
- Saves order to database
- Creates outbox event in the same transaction
- Ensures atomicity

### OutboxProcessor
Scheduled job that:
- Polls for pending events every 5 seconds
- Processes up to 10 events per batch
- Marks events as PROCESSED or FAILED
- Simulates message broker publishing

---

## Benefits of This Pattern

```
┌─────────────────────────────────────────────────────────────────┐
│  ✅ Guaranteed Event Delivery                                   │
│     Events are persisted atomically with business data         │
│                                                                 │
│  ✅ No Dual Write Problem                                       │
│     Single transaction ensures consistency                     │
│                                                                 │
│  ✅ Resilience                                                  │
│     Failed events can be retried automatically                 │
│                                                                 │
│  ✅ Audit Trail                                                 │
│     All events are logged in the database with timestamps      │
│                                                                 │
│  ✅ At-Least-Once Delivery                                      │
│     Events will be processed even if the application crashes   │
│                                                                 │
│  ✅ Decoupling                                                  │
│     Business logic is independent of message delivery          │
└─────────────────────────────────────────────────────────────────┘
```

---

## Real-World Extensions

In production, you would:

1. **Integrate with actual message brokers** (Kafka, RabbitMQ, AWS SNS/SQS)
2. **Add dead letter queue** for failed events after max retries
3. **Implement idempotency keys** for exactly-once processing
4. **Add monitoring and alerting** on outbox event metrics
5. **Consider using Debezium** for CDC-based outbox (no polling needed)
6. **Implement event versioning** and schema registry
7. **Add distributed locking** for multi-instance deployments
8. **Implement event partitioning** for scalability

---

## Stop the Application

```bash
# Stop the Spring Boot application
Ctrl+C

# Stop PostgreSQL
docker-compose down

# Stop and remove volumes (deletes all data)
docker-compose down -v
```

---

## Further Reading

- [Microservices.io - Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Debezium - Change Data Capture](https://debezium.io/)
- [Martin Fowler - Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)

---

## License

MIT