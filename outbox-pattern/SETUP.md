# Quick Setup Guide

## Start the Application in 3 Steps

### Option 1: Using Quick Start Script

```bash
./quick-start.sh
./gradlew bootRun
```

### Option 2: Manual Setup

1. **Start PostgreSQL**
```bash
docker-compose up -d
```

2. **Build the application**
```bash
./gradlew build
```

3. **Run the application**
```bash
./gradlew bootRun
```

## Test the Outbox Pattern

Once the application is running, execute the test script in another terminal:

```bash
./test-outbox.sh
```

Or manually create an order:

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

## Verify the Outbox Pattern

### Check Application Logs

You should see:
1. Order creation log
2. Outbox event creation log
3. Scheduled processor picking up the event (every 5 seconds)
4. Event publishing log
5. Event marked as PROCESSED

### Check Database

```bash
# Connect to PostgreSQL
docker exec -it outbox-postgres psql -U outboxuser -d outboxdb

# View orders
SELECT * FROM orders;

# View outbox events and their status
SELECT id, aggregate_type, event_type, status, created_at, processed_at
FROM outbox_events
ORDER BY created_at DESC;
```

You should see:
- Orders in the `orders` table
- Corresponding events in `outbox_events` table
- Events with `status = 'PROCESSED'` after the scheduler runs
- `processed_at` timestamp populated for processed events

## Stop Everything

```bash
# Stop the Spring Boot application
Ctrl+C (in the terminal running the application)

# Stop PostgreSQL
docker-compose down

# Stop PostgreSQL and delete data
docker-compose down -v
```

## Troubleshooting

### Port 5434 already in use

If you have another PostgreSQL instance running:

```bash
# Check what's using port 5434
lsof -i :5434

# Either stop that service or change the port in docker-compose.yml and application.properties
```

Note: This project uses port 5434 by default to avoid conflicts with other PostgreSQL instances that typically run on 5432.

### Port 8080 already in use

Change the port in `application.properties`:

```properties
server.port=8081
```

### Can't connect to database

Wait a few seconds for PostgreSQL to fully start:

```bash
# Check PostgreSQL logs
docker logs outbox-postgres

# Check if PostgreSQL is ready
docker exec outbox-postgres pg_isready -U outboxuser -d outboxdb
```

## Understanding the Flow

```
1. POST /api/orders
   ↓
2. OrderService.createOrder()
   ↓
3. ──────────────────────────────────
   │ Database Transaction          │
   │ - Save Order                  │
   │ - Save OutboxEvent (PENDING)  │
   └───────────────────────────────┘
   ↓
4. OutboxProcessor (every 5 seconds)
   ↓
5. Find PENDING events
   ↓
6. Publish to message broker (simulated)
   ↓
7. Mark event as PROCESSED
```

The key benefit: Steps 3 (save order + outbox event) happen in a single transaction, guaranteeing that if the order is saved, the event is also saved, and vice versa.