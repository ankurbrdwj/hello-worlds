#!/bin/bash

echo "======================================="
echo "Outbox Pattern - Quick Start"
echo "======================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

echo "Step 1: Starting PostgreSQL..."
docker-compose up -d

echo ""
echo "Step 2: Waiting for PostgreSQL to be ready..."
sleep 5

until docker exec outbox-postgres pg_isready -U outboxuser -d outboxdb > /dev/null 2>&1; do
    echo "Waiting for PostgreSQL..."
    sleep 2
done

echo ""
echo "PostgreSQL is ready!"
echo ""
echo "Step 3: Building the application..."
./gradlew build

echo ""
echo "======================================="
echo "Setup complete!"
echo "======================================="
echo ""
echo "To start the application, run:"
echo "  ./gradlew bootRun"
echo ""
echo "To test the API, run (in another terminal):"
echo "  ./test-outbox.sh"
echo ""
echo "To check the database:"
echo "  docker exec -it outbox-postgres psql -U outboxuser -d outboxdb"
echo ""
echo "To stop PostgreSQL:"
echo "  docker-compose down"
echo ""