#!/bin/bash

echo "======================================="
echo "Testing Outbox Pattern Implementation"
echo "======================================="
echo ""

echo "1. Creating first order..."
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "product": "Laptop",
    "quantity": 1,
    "totalPrice": 999.99
  }' | json_pp

echo ""
echo ""
echo "2. Creating second order..."
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Smith",
    "product": "Smartphone",
    "quantity": 2,
    "totalPrice": 1299.98
  }' | json_pp

echo ""
echo ""
echo "3. Creating third order..."
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Bob Johnson",
    "product": "Headphones",
    "quantity": 3,
    "totalPrice": 299.97
  }' | json_pp

echo ""
echo ""
echo "======================================="
echo "Orders created! Check application logs"
echo "to see outbox events being processed."
echo "======================================="
echo ""
echo "To verify in database, run:"
echo "docker exec -it outbox-postgres psql -U outboxuser -d outboxdb"
echo ""
echo "Then execute:"
echo "  SELECT * FROM orders;"
echo "  SELECT id, event_type, status, created_at, processed_at FROM outbox_events;"