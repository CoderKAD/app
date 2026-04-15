# 📊 RESERVATION CREATION FLOW - VISUAL GUIDE

## 🎯 Complete Workflow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          START: Create Reservation                      │
│                                                                         │
│  Input:  numberOfPeople, customerName, startAt, tableIds, duration    │
└─────────────────────────────────────────────────────────────────────────┘
                                    ⬇️
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 1: INPUT VALIDATION                                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Check:                                                                 │
│  ├─ numberOfPeople (1-50) ───────────────► ❌ FAIL? → Reject          │
│  ├─ customerName (not empty) ────────────► ❌ FAIL? → Reject          │
│  ├─ customerPhone (not empty) ──────────► ❌ FAIL? → Reject          │
│  ├─ startAt (provided) ─────────────────► ❌ FAIL? → Reject          │
│  └─ durationReservation (if provided) ──► ❌ FAIL? → Reject          │
│                                                                         │
│  ✓ ALL VALID → Continue to STEP 2                                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ⬇️
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 2: TIME CALCULATION                                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Duration Logic:                                                        │
│  ┌─────────────────────────────────────────────────────────────┐      │
│  │ IF durationReservation is provided AND > 0:                │      │
│  │   duration = durationReservation × 60 minutes              │      │
│  │ ELSE:                                                       │      │
│  │   duration = DEFAULT_DURATION (60 minutes = 1 hour)        │      │
│  └─────────────────────────────────────────────────────────────┘      │
│                                                                         │
│  Time Formula:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐      │
│  │ endAt = startAt + duration + 30_minutes_buffer             │      │
│  └─────────────────────────────────────────────────────────────┘      │
│                                                                         │
│  Example:                                                               │
│    startAt = 2026-04-20 19:00:00                                      │
│    duration = 2 hours = 120 minutes                                    │
│    buffer = 30 minutes                                                  │
│    ───────────────────────────────────────────                        │
│    endAt = 2026-04-20 21:30:00                                        │
│                                                                         │
│  ✓ Times calculated → Continue to STEP 3                             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ⬇️
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 3: BUSINESS HOURS VALIDATION (11:00 - 23:00)                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Checks:                                                                │
│  ├─ startAt >= 11:00 (Opening Time)                                   │
│  │  ❌ If BEFORE 11:00 → Reject ("Opens at 11:00")                   │
│  │                                                                      │
│  └─ endAt <= 23:00 (Closing Time)                                     │
│     ❌ If AFTER 23:00 → Reject ("Closes at 23:00")                   │
│                                                                         │
│  Timeline Example:                                                      │
│  ┌──────────────────────────────────────────────────────────┐         │
│  │ 11:00 AM ==================== 23:00 (11:00 PM) =======  │         │
│  │ (Opening)                         (Closing)              │         │
│  │                                                           │         │
│  │ Valid:   [✓] 14:00-16:00 (2 PM - 4 PM)                 │         │
│  │          [✓] 20:00-22:00 (8 PM - 10 PM)                │         │
│  │ Invalid: [✗] 10:00-12:00 (Starts before 11:00)        │         │
│  │          [✗] 22:00-00:30 (Ends after 23:00)            │         │
│  └──────────────────────────────────────────────────────────┘         │
│                                                                         │
│  ✓ WITHIN BUSINESS HOURS → Continue to STEP 4                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ⬇️
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 4: TABLE VALIDATION (3 Sub-Steps)                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│ 4.1) CHECK TABLE EXISTS & IS ACTIVE                                   │
│     ┌──────────────────────────────────────────────────────┐          │
│     │ For each tableId:                                    │          │
│     ├─ Query database for table                            │          │
│     │  ❌ If NOT FOUND → EntityNotFoundException           │          │
│     │                                                       │          │
│     ├─ Check table.active = TRUE                          │          │
│     │  ❌ If FALSE → "Table is not active"                │          │
│     │                                                       │          │
│     └─ ✓ PASS → Add to selectedTables, track seats        │          │
│     └──────────────────────────────────────────────────────┘          │
│                                                                         │
│ 4.2) CHECK TOTAL CAPACITY                                             │
│     ┌──────────────────────────────────────────────────────┐          │
│     │ Total Capacity = SUM(table.seats for all tables)     │          │
│     │                                                       │          │
│     │ IF totalCapacity < numberOfPeople:                   │          │
│     │   ❌ NoTablesAvailableException                      │          │
│     │                                                       │          │
│     │ Example:                                              │          │
│     │   Table A: 4 seats                                   │          │
│     │   Table B: 6 seats                                   │          │
│     │   ─────────────────                                  │          │
│     │   Total: 10 seats                                    │          │
│     │   Need: 8 people                                     │          │
│     │   Result: ✓ OK (10 >= 8)                            │          │
│     └──────────────────────────────────────────────────────┘          │
│                                                                         │
│ 4.3) CHECK FOR OVERLAPPING RESERVATIONS (CONFLICT DETECTION)         │
│     ┌──────────────────────────────────────────────────────┐          │
│     │ Overlap Rule:                                        │          │
│     │ ┌────────────────────────────────────────────┐      │          │
│     │ │ CONFLICT IF:                               │      │          │
│     │ │ (newStart < existingEnd) AND               │      │          │
│     │ │ (newEnd > existingStart)                   │      │          │
│     │ └────────────────────────────────────────────┘      │          │
│     │                                                       │          │
│     │ Timeline Examples:                                   │          │
│     │ ┌──────────────────────────────────────────┐        │          │
│     │ │ Existing: |═════════════════════| (10-12)│        │          │
│     │ │                                           │        │          │
│     │ │ New #1: |═══════| (09-11) → CONFLICT ❌  │        │          │
│     │ │ New #2:       |═════════| (11-13) → CONFLICT ❌  │          │
│     │ │ New #3:                 |═════════| (12-14) → OK ✓│        │          │
│     │ │ New #4: |═════|     (08-10) → OK ✓       │        │          │
│     │ └──────────────────────────────────────────┘        │          │
│     │                                                       │          │
│     │ IF any table has conflict:                           │          │
│     │   ❌ ReservationConflictException                    │          │
│     │                                                       │          │
│     │ ELSE:                                                │          │
│     │   ✓ ALL TABLES CLEAR FOR SELECTED TIME              │          │
│     └──────────────────────────────────────────────────────┘          │
│                                                                         │
│  ✓ ALL TABLE VALIDATIONS PASSED → Continue to STEP 5                 │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ⬇️
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 5: CREATE & SAVE RESERVATION                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Actions:                                                               │
│  ├─ Create new Reservation object                                     │
│  ├─ Set: numberOfPeople, customerName, customerPhone                 │
│  ├─ Set: startAt, endAt, durationReservation                        │
│  ├─ Set: status = ReservationStatus.PENDING                         │
│  ├─ Set: tables = selectedTables                                     │
│  ├─ Set: notes (if provided)                                         │
│  ├─ Generate code: ReservationCodeGenerator.generate(startAt)       │
│  ├─ Set: reservationCode = code (e.g., RES-20260420-K9P2X)         │
│  ├─ Set: createdBy = User (if provided)                             │
│  │                                                                    │
│  └─ SAVE to database (within @Transactional)                         │
│     └─ If any error: ROLLBACK all changes                           │
│                                                                         │
│  ✓ RESERVATION CREATED SUCCESSFULLY                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ⬇️
┌─────────────────────────────────────────────────────────────────────────┐
│                       END: Return ReservationResponseDto              │
│                                                                         │
│  Response contains:                                                     │
│  ├─ id: UUID                                                           │
│  ├─ reservationCode: "RES-20260420-K9P2X"                            │
│  ├─ numberOfPeople: 8                                                 │
│  ├─ customerName, customerPhone, emailCustomer                       │
│  ├─ startAt, endAt (with 30-min buffer included in endAt)           │
│  ├─ durationReservation: 2 (hours)                                   │
│  ├─ status: "PENDING"                                                 │
│  ├─ notes: (if any)                                                   │
│  └─ tableIds: [id1, id2, ...]                                        │
│                                                                         │
│  HTTP Status: 201 Created                                             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ❌ Error Paths

### **Error Path: Input Validation**
```
START: Create Reservation
  ⬇️
STEP 1: Input Validation
  ├─ numberOfPeople is NULL or < 1
  │  ❌ REJECT: "Number of people must be at least 1"
  │     Status: 400 Bad Request
  │
  ├─ customerName is empty
  │  ❌ REJECT: "Customer name is required"
  │     Status: 400 Bad Request
  │
  └─ durationReservation is -1
     ❌ REJECT: "Duration must be at least 1 hour"
        Status: 400 Bad Request

RESPONSE: Error message + no reservation created
```

### **Error Path: Business Hours**
```
START: Create Reservation
  ⬇️
STEP 1: Input Validation ✓
  ⬇️
STEP 2: Time Calculation ✓
  startAt = 2026-04-20 10:30 (10:30 AM)
  endAt = 2026-04-20 12:00 (12:00 PM)
  ⬇️
STEP 3: Business Hours Validation
  startAt (10:30) < OPENING_TIME (11:00)
  ❌ REJECT: "Restaurant opens at 11:00. Requested start: 10:30"
     Status: 400 Bad Request

RESPONSE: Error message + no reservation created
```

### **Error Path: Table Not Found**
```
START: Create Reservation
  ⬇️
STEP 1: Input Validation ✓
  ⬇️
STEP 2: Time Calculation ✓
  ⬇️
STEP 3: Business Hours Validation ✓
  ⬇️
STEP 4: Table Validation
  4.1) Table Existence
  
  tableId = "00000000-0000-0000-0000-000000000000"
  Query database: SELECT * FROM restaurant_tables WHERE id = ...
  Result: NOT FOUND
  
  ❌ REJECT: "Table not found with ID: 00000000-0000-0000-0000-000000000000"
     Status: 404 Not Found

RESPONSE: Error message + no reservation created
```

### **Error Path: Insufficient Capacity**
```
START: Create Reservation
  ⬇️
STEP 1-3: Validation ✓
  ⬇️
STEP 4: Table Validation
  4.1) Table Existence ✓
  4.2) Capacity Check
  
  Table A: 2 seats
  Table B: 3 seats
  Total: 5 seats
  
  numberOfPeople: 8
  
  5 < 8 ❌
  
  ❌ REJECT: "Insufficient table capacity. 
              Selected tables have 5 seats but you need 8 seats"
     Status: 400 Bad Request

RESPONSE: Error message + no reservation created
```

### **Error Path: Overlapping Reservation (Conflict)**
```
START: Create Reservation
  ⬇️
STEP 1-4.2: All validations ✓
  ⬇️
STEP 4.3: Overlap Detection
  
  New reservation: 2026-04-20 19:00 - 21:30
  Table A query results:
    Existing: 2026-04-20 20:00 - 22:00
    
  Check: (19:00 < 22:00) AND (21:30 > 20:00) → TRUE
  CONFLICT! ❌
  
  ❌ REJECT: "Table 'Main Room Table 5' is already reserved 
              from 2026-04-20T20:00 to 2026-04-20T22:00. 
              Your requested time: 2026-04-20T19:00 to 2026-04-20T21:30"
     Status: 409 Conflict

RESPONSE: Error message + no reservation created
```

---

## 🎯 Time Calculation Examples

### **Example 1: Default Duration**
```
Input:
  startAt: 2026-04-20 14:00 (2:00 PM)
  durationReservation: null (not provided)

Calculation:
  duration = DEFAULT_DURATION_MINUTES = 60 minutes = 1 hour
  buffer = 30 minutes
  endAt = 14:00 + 1h + 30min = 15:30

Result:
  startAt: 14:00 (2:00 PM)
  endAt: 15:30 (3:30 PM)
  Total occupied: 1.5 hours (1h reservation + 30min buffer)
```

### **Example 2: Custom Duration 2 Hours**
```
Input:
  startAt: 2026-04-20 18:00 (6:00 PM)
  durationReservation: 2

Calculation:
  duration = 2 hours × 60 = 120 minutes
  buffer = 30 minutes
  endAt = 18:00 + 2h + 30min = 20:30

Result:
  startAt: 18:00 (6:00 PM)
  endAt: 20:30 (8:30 PM)
  Total occupied: 2.5 hours (2h reservation + 30min buffer)
```

### **Example 3: Close to Closing Time**
```
Input:
  startAt: 2026-04-20 21:30 (9:30 PM)
  durationReservation: 1

Calculation:
  duration = 1 hour = 60 minutes
  buffer = 30 minutes
  endAt = 21:30 + 1h + 30min = 23:00

Check: endAt (23:00) <= CLOSING_TIME (23:00)? YES ✓

Result:
  startAt: 21:30 (9:30 PM)
  endAt: 23:00 (11:00 PM)
  Status: ✓ VALID (ends exactly at closing time)
```

### **Example 4: Exceeds Closing Time**
```
Input:
  startAt: 2026-04-20 22:00 (10:00 PM)
  durationReservation: 1

Calculation:
  duration = 1 hour = 60 minutes
  buffer = 30 minutes
  endAt = 22:00 + 1h + 30min = 23:30

Check: endAt (23:30) <= CLOSING_TIME (23:00)? NO ❌

Result:
  ❌ REJECT: "Restaurant closes at 23:00. 
              Requested end: 23:30 (includes 30-minute buffer)"
```

---

## 🔄 Capacity Check Examples

### **Example 1: Sufficient Capacity**
```
Selected Tables:
  Table A: 4 seats
  Table B: 6 seats
  Table C: 3 seats
  ─────────────────
  Total: 13 seats

Party Size: 10 people

Check: 13 >= 10? YES ✓

Result: ✓ PASS (3 seats wasted, but acceptable)
```

### **Example 2: Exact Capacity**
```
Selected Tables:
  Table A: 5 seats
  Table B: 5 seats
  ──────────────
  Total: 10 seats

Party Size: 10 people

Check: 10 >= 10? YES ✓

Result: ✓ PASS (perfect fit, no waste)
```

### **Example 3: Insufficient Capacity**
```
Selected Tables:
  Table A: 3 seats
  Table B: 4 seats
  ──────────────
  Total: 7 seats

Party Size: 10 people

Check: 7 >= 10? NO ❌

Result: ❌ FAIL ("Insufficient table capacity. 
               Selected tables have 7 seats but you need 10 seats")
```

---

## 📋 Status Codes Reference

| Status | Code | When Used | Example |
|--------|------|-----------|---------|
| Created | 201 | Reservation successfully created | "Reservation created with ID: ..." |
| Bad Request | 400 | Input validation fails | "Number of people must be at least 1" |
| Not Found | 404 | Table doesn't exist | "Table not found with ID: ..." |
| Conflict | 409 | Time slot already taken | "Table already reserved from..." |

---

**This visual guide helps understand the complete reservation creation process!** ✅
