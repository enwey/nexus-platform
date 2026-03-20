#!/bin/bash

echo "Initializing Nakama database..."

# Wait for PostgreSQL to be ready
until pg_isready -h postgres -U nexus -d nexus_platform; do
  echo "Waiting for PostgreSQL..."
  sleep 2
done

echo "PostgreSQL is ready!"

# Initialize Nakama database
docker exec -i nexus-nakama /nakama/nakama migrate up

echo "Nakama database initialized successfully!"
echo ""
echo "Default credentials:"
echo "  Server Key: defaultkeychanged"
echo "  HTTP Key: defaulthttpkeychanged"
echo "  Admin Email: admin@example.com"
echo "  Admin Password: password"
echo ""
echo "Please change these credentials in production!"
