#!/bin/bash

BACKUP_DIR="./backups"
DATE=$(date +%Y%m%d_%H%M%S)

echo "Starting backup..."

# Create backup directory if it doesn't exist
mkdir -p $BACKUP_DIR

# Backup PostgreSQL
echo "Backing up PostgreSQL..."
docker exec nexus-postgres pg_dump -U nexus -d nexus_platform > $BACKUP_DIR/postgres_$DATE.sql

# Backup MinIO
echo "Backing up MinIO..."
docker exec nexus-minio mc mirror minio/ $BACKUP_DIR/minio_$DATE

# Backup Nakama
echo "Backing up Nakama..."
docker exec nexus-nakama /nakama/nakama migrate export > $BACKUP_DIR/nakama_$DATE.sql

# Backup Backend configuration
echo "Backing up Backend configuration..."
cp ../backend/src/main/resources/application.yml $BACKUP_DIR/application_$DATE.yml

# Compress backups
echo "Compressing backups..."
cd $BACKUP_DIR
tar -czf nexus_backup_$DATE.tar.gz *.sql *.yml

# Clean up old backups (keep last 7 days)
echo "Cleaning up old backups..."
find $BACKUP_DIR -name "nexus_backup_*.tar.gz" -mtime +7 -delete

echo "Backup completed: nexus_backup_$DATE.tar.gz"
echo "Backup location: $BACKUP_DIR"
