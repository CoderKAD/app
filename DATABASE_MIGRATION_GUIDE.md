# Database Migration Guide for Reservation System Update

## Overview
This guide provides SQL scripts for migrating from the old reservation schema to the new production-ready schema.

---

## Pre-Migration Checklist

- [ ] Full database backup created
- [ ] Staging environment tested
- [ ] Rollback plan documented
- [ ] Downtime window scheduled
- [ ] Team notified
- [ ] Monitoring ready

---

## Step 1: Add New Columns (Nullable)

Run these scripts to add new columns. They're nullable initially to maintain data integrity.

```sql
-- Add new business logic columns
ALTER TABLE reservations 
ADD COLUMN IF NOT EXISTS buffer_time_minutes INTEGER DEFAULT 15;

ALTER TABLE reservations 
ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP NULL;

ALTER TABLE reservations 
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP NULL;

ALTER TABLE reservations 
ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(255) NULL;

-- Update column constraint for status to include new values
-- (PostgreSQL doesn't require this, but document that ENUM needs extension)
-- PENDING, CONFIRMED, NO_SHOW, COMPLETED, CANCELLED
```

---

## Step 2: Rename Columns

**IMPORTANT**: These renames break backward compatibility. Update all application code first!

### PostgreSQL (Recommended - if using PostgreSQL)

```sql
-- Rename partySize to numberOfPeople
ALTER TABLE reservations 
RENAME COLUMN party_size TO number_of_people;

-- Rename customerEmail to emailCustomer
ALTER TABLE reservations 
RENAME COLUMN customer_email TO email_customer;

-- Rename durationMinutes to durationReservation
-- NOTE: Also change data type from minutes (INTEGER) to hours (INTEGER)
-- First create new column with new name
ALTER TABLE reservations 
ADD COLUMN duration_reservation_temp INTEGER;

-- Copy data, converting minutes to hours
UPDATE reservations 
SET duration_reservation_temp = CEIL(duration_minutes::FLOAT / 60)
WHERE duration_minutes IS NOT NULL;

-- Drop old column
ALTER TABLE reservations 
DROP COLUMN duration_minutes;

-- Rename temporary column
ALTER TABLE reservations 
RENAME COLUMN duration_reservation_temp TO duration_reservation;

-- Set default value
ALTER TABLE reservations 
ALTER COLUMN duration_reservation SET DEFAULT 1;
```

### MySQL (If using MySQL)

```sql
-- Rename partySize to numberOfPeople
ALTER TABLE reservations 
CHANGE COLUMN party_size number_of_people INT;

-- Rename customerEmail to emailCustomer
ALTER TABLE reservations 
CHANGE COLUMN customer_email email_customer VARCHAR(255);

-- Rename and convert durationMinutes to durationReservation
ALTER TABLE reservations 
ADD COLUMN duration_reservation INT;

UPDATE reservations 
SET duration_reservation = CEIL(duration_minutes / 60)
WHERE duration_minutes IS NOT NULL;

ALTER TABLE reservations 
DROP COLUMN duration_minutes;

ALTER TABLE reservations 
MODIFY COLUMN duration_reservation INT DEFAULT 1;
```

---

## Step 3: Update NOT NULL Constraints

After verifying data migration, add NOT NULL constraints:

```sql
-- Reservation code should be unique and not null
ALTER TABLE reservations 
ALTER COLUMN reservation_code SET NOT NULL;

ALTER TABLE reservations 
ADD CONSTRAINT uk_reservation_code UNIQUE (reservation_code);

-- Number of people should not be null
ALTER TABLE reservations 
ALTER COLUMN number_of_people SET NOT NULL;

-- Start and end times should not be null
ALTER TABLE reservations 
ALTER COLUMN start_at SET NOT NULL;

ALTER TABLE reservations 
ALTER COLUMN end_at SET NOT NULL;

-- Status should not be null
ALTER TABLE reservations 
ALTER COLUMN status SET NOT NULL;
```

---

## Step 4: Create New Indexes

These indexes optimize the new query patterns:

```sql
-- Index for time range queries
CREATE INDEX IF NOT EXISTS idx_reservations_end_at 
ON reservations(end_at);

-- Index for status-based queries
CREATE INDEX IF NOT EXISTS idx_reservations_status 
ON reservations(status);

-- Index for combined status and start time queries
CREATE INDEX IF NOT EXISTS idx_reservations_status_start 
ON reservations(status, start_at);

-- Index for availability checking (time range + status)
CREATE INDEX IF NOT EXISTS idx_reservations_time_range 
ON reservations(start_at, end_at);

-- Index for quick lookup by code
CREATE INDEX IF NOT EXISTS idx_reservation_code 
ON reservations(reservation_code);

-- Composite index for overlap detection (table_id + time range)
-- This requires knowledge of reservation_tables join structure
CREATE INDEX IF NOT EXISTS idx_reservation_tables_table_time
ON reservations(start_at, end_at) 
WHERE status IN ('PENDING', 'CONFIRMED');
```

---

## Step 5: Verify Migration

Run these verification queries to ensure data integrity:

```sql
-- Check no NULL values in required columns
SELECT COUNT(*) as null_count FROM reservations 
WHERE reservation_code IS NULL;

SELECT COUNT(*) as null_count FROM reservations 
WHERE number_of_people IS NULL;

SELECT COUNT(*) as null_count FROM reservations 
WHERE start_at IS NULL OR end_at IS NULL;

-- Verify duration conversion (should be reasonable numbers, 1-8 hours typically)
SELECT MIN(duration_reservation), MAX(duration_reservation) 
FROM reservations;

-- Check for any issues with new status values
SELECT DISTINCT status FROM reservations;
-- Should only show: PENDING, CONFIRMED, CANCELLED
-- After update, can have: NO_SHOW, COMPLETED

-- Verify unique constraint on reservation_code
SELECT COUNT(*) as duplicate_codes FROM (
  SELECT reservation_code, COUNT(*) 
  FROM reservations 
  WHERE reservation_code IS NOT NULL 
  GROUP BY reservation_code 
  HAVING COUNT(*) > 1
) duplicates;
-- Should return 0

-- Check index usage
-- PostgreSQL
EXPLAIN ANALYZE 
SELECT * FROM reservations 
WHERE start_at > NOW() AND end_at < NOW() + INTERVAL '1 day' 
AND status = 'PENDING';

-- MySQL
EXPLAIN 
SELECT * FROM reservations 
WHERE start_at > NOW() AND end_at < DATE_ADD(NOW(), INTERVAL 1 DAY) 
AND status = 'PENDING';
```

---

## Step 6: Update Views (If Any)

If your application uses database views, update their column references:

```sql
-- Example: If you have a view for available reservations
CREATE OR REPLACE VIEW v_confirmed_reservations AS
SELECT 
    id,
    reservation_code,
    number_of_people,
    customer_name,
    customer_phone,
    email_customer,
    start_at,
    end_at,
    status
FROM reservations
WHERE status = 'CONFIRMED'
ORDER BY start_at DESC;
```

---

## Step 7: Update Application Code

Update your application configuration:

### application.properties or application.yml

```properties
# Enable Hibernate dialect for new features
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL13Dialect

# Or for MySQL
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Enable proper handling of enums
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

### Build and Deploy

```bash
# Clean build
mvn clean package

# Run tests
mvn test

# Deploy (method varies by setup)
```

---

## Step 8: Verify Post-Deployment

After deploying the application, verify everything works:

```bash
# Test endpoint connectivity
curl -X GET http://localhost:8080/api/reservations

# Test creating a reservation
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "numberOfPeople": 4,
    "customerName": "Test",
    "customerPhone": "+1234567890",
    "startAt": "2026-04-20T19:00:00",
    "endAt": "2026-04-20T21:00:00",
    "durationReservation": 2,
    "status": "PENDING",
    "tableIds": ["table-uuid"]
  }'

# Check logs for errors
tail -f /var/log/app/application.log
```

---

## Rollback Procedure

If something goes wrong, here's how to rollback:

### PostgreSQL Rollback

```sql
-- Drop new constraints and columns
ALTER TABLE reservations DROP CONSTRAINT uk_reservation_code;
ALTER TABLE reservations ALTER COLUMN reservation_code DROP NOT NULL;

-- Reverse column renames
ALTER TABLE reservations RENAME COLUMN number_of_people TO party_size;
ALTER TABLE reservations RENAME COLUMN email_customer TO customer_email;
ALTER TABLE reservations RENAME COLUMN duration_reservation TO duration_minutes;

-- Drop new columns
ALTER TABLE reservations DROP COLUMN buffer_time_minutes;
ALTER TABLE reservations DROP COLUMN confirmed_at;
ALTER TABLE reservations DROP COLUMN cancelled_at;
ALTER TABLE reservations DROP COLUMN cancel_reason;

-- Drop new indexes
DROP INDEX IF EXISTS idx_reservations_end_at;
DROP INDEX IF EXISTS idx_reservations_status;
DROP INDEX IF EXISTS idx_reservations_status_start;
DROP INDEX IF EXISTS idx_reservations_time_range;
DROP INDEX IF EXISTS idx_reservation_code;
DROP INDEX IF EXISTS idx_reservation_tables_table_time;

-- Restore from backup if needed
-- psql restaurantdb < backup_20260415_120000.sql
```

### Application Rollback

```bash
# Revert to previous version
# Method depends on your deployment setup

# For Docker:
docker rollback app-service

# For traditional servers:
# Redeploy previous version from artifact repository
```

---

## Testing Migration Script

### Create Test Environment

```sql
-- Create backup of current data
CREATE TABLE reservations_backup AS SELECT * FROM reservations;
CREATE TABLE reservation_tables_backup AS SELECT * FROM reservation_tables;

-- Test migration on backup
ALTER TABLE reservations_backup RENAME TO reservations_test;

-- Run migration steps on _test table
-- Verify results
-- If OK, apply to production
```

---

## Schema Comparison

### Before Migration
```sql
CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    party_size INTEGER,
    reservation_code VARCHAR(255),
    customer_name VARCHAR(50),
    customer_phone VARCHAR(20),
    customer_email VARCHAR(255),
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    status VARCHAR(50),
    notes TEXT,
    duration_minutes INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);
```

### After Migration
```sql
CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    number_of_people INTEGER NOT NULL,
    reservation_code VARCHAR(255) NOT NULL UNIQUE,
    customer_name VARCHAR(50) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    email_customer VARCHAR(255),
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    duration_reservation INTEGER DEFAULT 1,
    buffer_time_minutes INTEGER DEFAULT 15,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancel_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_reservations_start_at ON reservations(start_at);
CREATE INDEX idx_reservations_end_at ON reservations(end_at);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservation_code ON reservations(reservation_code);
CREATE INDEX idx_reservations_status_start ON reservations(status, start_at);
CREATE INDEX idx_reservations_time_range ON reservations(start_at, end_at);
```

---

## Performance Tuning (Post-Migration)

After successful migration, optimize performance:

### PostgreSQL
```sql
-- Analyze tables to update statistics
ANALYZE reservations;
ANALYZE reservation_tables;

-- Vacuum to clean up
VACUUM ANALYZE reservations;

-- Check index fragmentation
SELECT schemaname, tablename, indexname, idx_size 
FROM pg_indexes 
WHERE tablename = 'reservations';
```

### MySQL
```sql
-- Optimize tables
OPTIMIZE TABLE reservations;
OPTIMIZE TABLE reservation_tables;

-- Analyze tables
ANALYZE TABLE reservations;
```

---

## Monitoring Post-Migration

### Key Metrics to Monitor

1. **Query Performance**
   - Availability check queries < 50ms
   - Reservation retrieval < 100ms
   - Confirmation < 150ms

2. **Error Rates**
   - Should be near zero for known queries
   - Watch for NULL constraint violations
   - Monitor for type conversion errors

3. **Application Logs**
   - No mapping errors
   - Proper enum handling
   - No index scan warnings

### Sample Monitoring Query (PostgreSQL)

```sql
-- Check slow queries
SELECT query, calls, mean_exec_time, max_exec_time 
FROM pg_stat_statements 
WHERE query LIKE '%reservations%' 
ORDER BY mean_exec_time DESC;
```

---

## Troubleshooting Migration Issues

### Issue: Column Already Exists
```sql
-- Use IF NOT EXISTS (PostgreSQL 10+)
ALTER TABLE reservations 
ADD COLUMN IF NOT EXISTS buffer_time_minutes INTEGER DEFAULT 15;
```

### Issue: Type Conversion Errors
```sql
-- Explicitly cast during conversion
UPDATE reservations 
SET duration_reservation = CAST(CEIL(duration_minutes::FLOAT / 60) AS INTEGER)
WHERE duration_minutes IS NOT NULL;
```

### Issue: Unique Constraint Violation on Code
```sql
-- Find duplicates
SELECT reservation_code, COUNT(*) 
FROM reservations 
WHERE reservation_code IS NOT NULL 
GROUP BY reservation_code 
HAVING COUNT(*) > 1;

-- Generate unique codes for duplicates
UPDATE reservations 
SET reservation_code = 'RES-' || to_char(NOW(), 'YYYYMMDD') || '-' || SUBSTR(MD5(RANDOM()::TEXT), 1, 5)
WHERE reservation_code IS NULL OR 
      reservation_code IN (SELECT reservation_code FROM reservations 
                            WHERE reservation_code IS NOT NULL 
                            GROUP BY reservation_code HAVING COUNT(*) > 1);
```

---

## Sign-Off

- [ ] Migration plan reviewed by DBA
- [ ] Backup verified
- [ ] Test migration successful
- [ ] Application tested with migrated schema
- [ ] Rollback procedure documented
- [ ] Team briefed on changes
- [ ] Monitoring configured
- [ ] Production migration scheduled

---

**Migration Status: Ready for Execution** ✅

For questions or issues, refer to the main documentation or contact the development team.
