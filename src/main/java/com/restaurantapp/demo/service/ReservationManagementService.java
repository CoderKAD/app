package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.ReservationDemandResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationDemandRequestDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.ReservationDemand;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import com.restaurantapp.demo.exception.InvalidReservationStatusException;
import com.restaurantapp.demo.exception.NoTablesAvailableException;
import com.restaurantapp.demo.exception.ReservationConflictException;
import com.restaurantapp.demo.mapper.ReservationDemandMapper;
import com.restaurantapp.demo.mapper.ReservationMapper;
import com.restaurantapp.demo.repository.ReservationDemandRepository;
import com.restaurantapp.demo.repository.ReservationRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import com.restaurantapp.demo.util.ReservationCodeGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Production-ready Reservation Management Service with:
 * - Table availability checking with overlapping detection
 * - Multi-table reservation support with greedy table selection algorithm
 * - Concurrent reservation handling with pessimistic locking
 * - Comprehensive status workflow
 * - Reservation code generation
 * - Business hours validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationManagementService {

    private final ReservationRepository reservationRepository;
    private final ReservationDemandRepository reservationDemandRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationDemandMapper reservationDemandMapper;

    // Business configuration
    private static final LocalTime OPENING_TIME = LocalTime.of(11, 0);  // 11:00 AM
    private static final LocalTime CLOSING_TIME = LocalTime.of(23, 0);  // 11:00 PM
    private static final int DEFAULT_DURATION_MINUTES = 60;             // 1 hour
    private static final int BUFFER_TIME_MINUTES = 30;                  // 30 min cleaning buffer
    private static final int DEFAULT_BUFFER_TIME_MINUTES = 15;
    private static final long PENDING_RESERVATION_TIMEOUT_MINUTES = 24 * 60; // 24 hours

    // ==================== AVAILABILITY CHECKING ====================

    /**
     * Checks if tables are available for the requested time slot.
     * Returns true if at least one table can accommodate the party.
     */
    public boolean isAvailable(Integer numberOfPeople, LocalDateTime startAt, LocalDateTime endAt) {
        validateBusinessHours(startAt, endAt);
        
        long count = restaurantTableRepository.countAvailableTablesForTimeSlot(
                numberOfPeople, startAt, endAt);
        return count > 0;
    }

    /**
     * Finds available tables for the requested time slot using greedy algorithm.
     * Returns tables sorted by capacity (ascending) to optimize table usage.
     */
    public List<RestaurantTable> findAvailableTables(Integer numberOfPeople, LocalDateTime startAt, LocalDateTime endAt) {
        validateBusinessHours(startAt, endAt);
        
        return restaurantTableRepository.findAvailableTablesForTimeSlot(
                numberOfPeople, startAt, endAt);
    }

    /**
     * Validates that the reservation fits within business hours.
     */
    private void validateBusinessHours(LocalDateTime startAt, LocalDateTime endAt) {
        LocalTime startTime = startAt.toLocalTime();
        LocalTime endTime = endAt.toLocalTime();

        if (startTime.isBefore(OPENING_TIME)) {
            throw new IllegalArgumentException(
                    "Reservation must start after " + OPENING_TIME + ". Requested: " + startTime);
        }

        if (endTime.isAfter(CLOSING_TIME)) {
            throw new IllegalArgumentException(
                    "Reservation must end by " + CLOSING_TIME + ". Requested: " + endTime);
        }
    }

    // ==================== RESERVATION CREATION ====================

    /**
     * Creates a new reservation with user-selected tables.
     * 
     * BUSINESS LOGIC:
     * 1. User provides: startAt, numberOfPeople, selected tables
     * 2. System checks:
     *    - startAt and endAt are within business hours (11:00 - 23:00)
     *    - Default duration is 1 hour if not provided
     *    - Add 30 minutes buffer for cleaning after reservation
     *    - endAt = startAt + duration + 30 minutes buffer
     * 
     * 3. Validations:
     *    - Check if selected tables exist and are active
     *    - Check if tables are available (no overlapping reservations)
     *    - Check if total capacity of tables >= numberOfPeople
     * 
     * 4. Overlap Detection Rule:
     *    - Conflict if: (startAt < existing endAt) AND (endAt > existing startAt)
     * 
     * 5. If all checks pass:
     *    - Create reservation with status PENDING
     *    - Assign selected tables
     * 
     * 6. If any check fails:
     *    - Reject with specific error message
     * 
     * @param dto ReservationRequestDto with startAt, numberOfPeople, tableIds
     * @return ReservationResponseDto with created reservation
     * @throws IllegalArgumentException if business hours violated or duration invalid
     * @throws EntityNotFoundException if table not found
     * @throws ReservationConflictException if tables have overlapping reservations
     * @throws NoTablesAvailableException if total capacity insufficient
     */
    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto dto) {
        log.info("=== CREATING RESERVATION ===");
        log.info("Request: {} people, start: {}, tables: {}", 
                 dto.getNumberOfPeople(), dto.getStartAt(), dto.getTableIds());
        
        // STEP 1: VALIDATE INPUT
        log.info("STEP 1: Validating input...");
        validateReservationInput(dto);
        
        // STEP 2: CALCULATE TIME WITH DURATION AND BUFFER
        log.info("STEP 2: Calculating reservation time...");
        LocalDateTime startAt = dto.getStartAt();
        
        // Use provided duration or default to 1 hour
        int durationMinutes = (dto.getDurationReservation() != null && dto.getDurationReservation() > 0) 
            ? dto.getDurationReservation() * 60  // Convert hours to minutes
            : DEFAULT_DURATION_MINUTES;
        
        // Calculate endAt = startAt + duration + 30 minutes buffer for cleaning
        LocalDateTime endAt = startAt.plusMinutes(durationMinutes).plusMinutes(BUFFER_TIME_MINUTES);
        
        log.info("Time calculation: start={}, duration={}h, buffer={}min, end={}", 
                 startAt, durationMinutes/60, BUFFER_TIME_MINUTES, endAt);
        
        // STEP 3: VALIDATE BUSINESS HOURS (11:00 - 23:00)
        log.info("STEP 3: Validating business hours (11:00 - 23:00)...");
        validateBusinessHours(startAt, endAt);
        
        // STEP 4: GET AND VALIDATE SELECTED TABLES
        log.info("STEP 4: Validating selected tables...");
        List<RestaurantTable> selectedTables = validateAndGetTables(
            dto.getTableIds(), 
            dto.getNumberOfPeople(), 
            startAt, 
            endAt
        );
        
        // STEP 5: CREATE AND SAVE RESERVATION
        log.info("STEP 5: Creating reservation entity...");
        Reservation reservation = new Reservation();
        reservation.setNumberOfPeople(dto.getNumberOfPeople());
        reservation.setCustomerName(dto.getCustomerName());
        reservation.setCustomerPhone(dto.getCustomerPhone());
        reservation.setEmailCustomer(dto.getEmailCustomer());
        reservation.setStartAt(startAt);
        reservation.setEndAt(endAt);
        reservation.setDurationReservation(durationMinutes / 60);  // Store in hours
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setNotes(dto.getNotes());
        reservation.setTables(selectedTables);
        
        // Generate unique reservation code
        String code = ReservationCodeGenerator.generate(startAt);
        reservation.setReservationCode(code);
        
        // Set user who created this reservation
        if (dto.getCreatedById() != null) {
            User creator = findUserById(dto.getCreatedById());
            reservation.setCreatedBy(creator);
            reservation.setUpdatedBy(creator);
        }
        
        // Save to database
        Reservation saved = reservationRepository.save(reservation);
        log.info("✅ RESERVATION CREATED SUCCESSFULLY");
        log.info("Reservation ID: {}, Code: {}, Status: PENDING", saved.getId(), saved.getReservationCode());
        
        return reservationMapper.toDto(saved);
    }

    /**
     * STEP 4: Validates selected tables and checks:
     * 1. All tables exist and are active
     * 2. No overlapping reservations (conflict detection)
     * 3. Total capacity >= numberOfPeople
     * 
     * Overlap Rule: A conflict exists if (newStart < existingEnd) AND (newEnd > existingStart)
     */
    private List<RestaurantTable> validateAndGetTables(
            List<UUID> tableIds, 
            Integer numberOfPeople, 
            LocalDateTime startAt, 
            LocalDateTime endAt) {
        
        log.info("Validating {} selected tables for {} people", tableIds.size(), numberOfPeople);
        
        if (tableIds == null || tableIds.isEmpty()) {
            log.error("❌ No tables selected");
            throw new IllegalArgumentException("At least one table must be selected");
        }
        
        List<RestaurantTable> selectedTables = new ArrayList<>();
        int totalCapacity = 0;
        
        // VALIDATION 1: Check all tables exist and are active
        for (UUID tableId : tableIds) {
            RestaurantTable table = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> {
                    log.error("❌ Table not found: {}", tableId);
                    return new EntityNotFoundException("Table not found with ID: " + tableId);
                });
            
            // Check if table is active
            if (!Boolean.TRUE.equals(table.getActive())) {
                log.error("❌ Table {} is not active", tableId);
                throw new IllegalArgumentException("Table " + table.getLabel() + " is not active");
            }
            
            selectedTables.add(table);
            totalCapacity += (table.getSeats() != null ? table.getSeats() : 0);
            log.debug("Table '{}' found: {} seats", table.getLabel(), table.getSeats());
        }
        
        // VALIDATION 2: Check if total capacity is sufficient
        log.info("Total capacity check: {} seats for {} people", totalCapacity, numberOfPeople);
        if (totalCapacity < numberOfPeople) {
            log.error("❌ Insufficient capacity: {} seats < {} people needed", totalCapacity, numberOfPeople);
            throw new NoTablesAvailableException(
                "Insufficient table capacity. Selected tables have " + totalCapacity + 
                " seats but you need " + numberOfPeople + " seats"
            );
        }
        log.info("✓ Capacity OK: {} seats available for {} people", totalCapacity, numberOfPeople);
        
        // VALIDATION 3: Check for overlapping reservations
        log.info("Checking for overlapping reservations...");
        for (RestaurantTable table : selectedTables) {
            List<Reservation> overlappingReservations = reservationRepository
                .findOverlappingReservations(table.getId(), startAt, endAt);
            
            if (!overlappingReservations.isEmpty()) {
                log.error("❌ Table '{}' has {} overlapping reservation(s)", table.getLabel(), overlappingReservations.size());
                
                Reservation conflict = overlappingReservations.get(0);
                String conflictMsg = String.format(
                    "Table '%s' is already reserved from %s to %s. " +
                    "Your requested time: %s to %s",
                    table.getLabel(),
                    conflict.getStartAt(),
                    conflict.getEndAt(),
                    startAt,
                    endAt
                );
                throw new ReservationConflictException(conflictMsg);
            }
            log.debug("✓ Table '{}' is available for selected time", table.getLabel());
        }
        
        log.info("✓ All validations passed. {} tables are available", selectedTables.size());
        return selectedTables;
    }

    /**
     * Custom validation for reservation input data
     */
    private void validateReservationInput(ReservationRequestDto dto) {
        // Validate number of people
        if (dto.getNumberOfPeople() == null || dto.getNumberOfPeople() < 1) {
            throw new IllegalArgumentException("Number of people must be at least 1");
        }
        if (dto.getNumberOfPeople() > 50) {
            throw new IllegalArgumentException("Maximum 50 people allowed per reservation");
        }
        
        // Validate customer name
        if (dto.getCustomerName() == null || dto.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        
        // Validate customer phone
        if (dto.getCustomerPhone() == null || dto.getCustomerPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer phone is required");
        }
        
        // Validate start date/time
        if (dto.getStartAt() == null) {
            throw new IllegalArgumentException("Reservation start date/time is required");
        }
        
        // Validate duration if provided
        if (dto.getDurationReservation() != null && dto.getDurationReservation() < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 hour");
        }
        
        log.info("✓ Input validation passed");
    }

    /**
     * Validates that reservation time is within business hours (11:00 - 23:00)
     * 
     * @param startAt Reservation start time
     * @param endAt Reservation end time (including buffer)
     * @throws IllegalArgumentException if times are outside business hours
     */
    private void validateBusinessHours(LocalDateTime startAt, LocalDateTime endAt) {
        LocalTime startTime = startAt.toLocalTime();
        LocalTime endTime = endAt.toLocalTime();
        
        log.info("Business hours check: {} to {}", startTime, endTime);
        
        // Check if start is at or after 11:00
        if (startTime.isBefore(OPENING_TIME)) {
            log.error("❌ Reservation starts before opening time ({})", OPENING_TIME);
            throw new IllegalArgumentException(
                "Restaurant opens at " + OPENING_TIME + ". Requested start: " + startTime
            );
        }
        
        // Check if end is at or before 23:00
        if (endTime.isAfter(CLOSING_TIME)) {
            log.error("❌ Reservation ends after closing time ({})", CLOSING_TIME);
            throw new IllegalArgumentException(
                "Restaurant closes at " + CLOSING_TIME + ". Requested end: " + endTime + 
                " (includes 30-minute buffer)"
            );
        }
        
        log.info("✓ Business hours validation passed");
    }

    // ==================== RESERVATION CONFIRMATION ====================

    /**
     * Confirms a PENDING reservation.
     * Uses pessimistic write lock to handle concurrent confirmations.
     * 
     * Transitions: PENDING -> CONFIRMED
     */
    @Transactional
    public ReservationResponseDto confirmReservation(UUID reservationId, UUID confirmedByUserId) {
        log.info("Confirming reservation: {}", reservationId);

        // Lock the reservation to prevent concurrent updates
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + reservationId));

        // Verify status
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStatusException(
                    "Cannot confirm reservation in " + reservation.getStatus() + " status");
        }

        // Final availability check to ensure no conflicts occurred
        List<Reservation> conflicts = reservationRepository.findOverlappingReservationsExcluding(
                reservation.getTables().get(0).getId(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservationId);

        if (!conflicts.isEmpty()) {
            throw new ReservationConflictException("Reservation time slot is no longer available");
        }

        // Update status
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        
        if (confirmedByUserId != null) {
            User confirmedBy = findUserById(confirmedByUserId);
            reservation.setUpdatedBy(confirmedBy);
        }

        Reservation confirmed = reservationRepository.save(reservation);
        log.info("Reservation confirmed: {}", reservationId);
        
        return reservationMapper.toDto(confirmed);
    }

    // ==================== RESERVATION CANCELLATION ====================

    /**
     * Cancels a reservation with reason.
     * Allowed only for PENDING or CONFIRMED reservations.
     * 
     * Transitions: PENDING -> CANCELLED, CONFIRMED -> CANCELLED
     */
    @Transactional
    public ReservationResponseDto cancelReservation(UUID reservationId, String reason, UUID cancelledByUserId) {
        log.info("Cancelling reservation: {}", reservationId);

        Reservation reservation = findReservationById(reservationId);

        // Check if cancellation is allowed
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidReservationStatusException("Reservation is already cancelled");
        }

        if (reservation.getStatus() == ReservationStatus.NO_SHOW || 
            reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new InvalidReservationStatusException(
                    "Cannot cancel reservation in " + reservation.getStatus() + " status");
        }

        // Update status
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancelReason(reason);

        if (cancelledByUserId != null) {
            User cancelledBy = findUserById(cancelledByUserId);
            reservation.setUpdatedBy(cancelledBy);
        }

        Reservation cancelled = reservationRepository.save(reservation);
        log.info("Reservation cancelled: {}", reservationId);
        
        return reservationMapper.toDto(cancelled);
    }

    // ==================== RESERVATION UPDATES ====================

    /**
     * Updates an existing reservation.
     * Only PENDING reservations can be updated with new times/tables.
     */
    @Transactional
    public ReservationResponseDto updateReservation(UUID id, ReservationRequestDto dto) {
        log.info("Updating reservation: {}", id);

        Reservation reservation = findReservationById(id);

        // Only pending reservations can be updated
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStatusException(
                    "Cannot update reservation in " + reservation.getStatus() + " status");
        }

        // Validate new time
        validateReservationInput(dto);
        validateBusinessHours(dto.getStartAt(), dto.getEndAt());

        // Check if new time slot is available (excluding current reservation)
        if (!reservation.getTables().isEmpty()) {
            List<Reservation> conflicts = reservationRepository.findOverlappingReservationsExcluding(
                    reservation.getTables().get(0).getId(),
                    dto.getStartAt(),
                    dto.getEndAt(),
                    id);

            if (!conflicts.isEmpty()) {
                throw new ReservationConflictException(
                        "New time slot is not available. Conflicts with other reservations");
            }
        }

        // Update reservation
        reservationMapper.updateEntity(dto, reservation);

        if (dto.getUpdatedById() != null) {
            User updater = findUserById(dto.getUpdatedById());
            reservation.setUpdatedBy(updater);
        }

        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation updated: {}", id);
        
        return reservationMapper.toDto(updated);
    }

    /**
     * Retrieves a reservation by ID.
     */
    public ReservationResponseDto getReservationById(UUID id) {
        return reservationMapper.toDto(findReservationById(id));
    }

    /**
     * Retrieves a reservation by its code.
     */
    public ReservationResponseDto getReservationByCode(String code) {
        Reservation reservation = reservationRepository.findByReservationCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found with code: " + code));
        return reservationMapper.toDto(reservation);
    }

    /**
     * Retrieves all reservations.
     */
    public List<ReservationResponseDto> getAllReservations() {
        return reservationMapper.toDto(reservationRepository.findAll());
    }

    /**
     * Retrieves all reservations for a customer phone number.
     */
    public List<ReservationResponseDto> getReservationsByCustomerPhone(String phone) {
        return reservationMapper.toDto(reservationRepository.findByCustomerPhone(phone));
    }

    /**
     * Retrieves upcoming reservations for a user.
     */
    public List<ReservationResponseDto> getUpcomingReservations(UUID userId) {
        return reservationMapper.toDto(
                reservationRepository.findUpcomingReservationsForUser(userId, LocalDateTime.now()));
    }

    /**
     * Deletes a reservation (only PENDING or CANCELLED).
     */
    @Transactional
    public void deleteReservation(UUID id) {
        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING && 
            reservation.getStatus() != ReservationStatus.CANCELLED) {
            throw new InvalidReservationStatusException(
                    "Cannot delete reservation in " + reservation.getStatus() + " status");
        }

        reservationRepository.deleteById(id);
        log.info("Reservation deleted: {}", id);
    }

    // ==================== AUTO-CANCELLATION ====================

    /**
     * Auto-cancels pending reservations that have exceeded the timeout threshold.
     * Should be called by a scheduled task.
     * 
     * Default timeout: 24 hours
     */
    @Transactional
    public int autoCancelPendingReservations() {
        LocalDateTime timeoutThreshold = LocalDateTime.now()
                .minusMinutes(PENDING_RESERVATION_TIMEOUT_MINUTES);

        List<Reservation> expiredReservations = reservationRepository
                .findExpiredPendingReservations(timeoutThreshold);

        int cancelledCount = 0;
        for (Reservation reservation : expiredReservations) {
            try {
                cancelReservation(reservation.getId(), 
                        "Auto-cancelled due to timeout", null);
                cancelledCount++;
            } catch (Exception e) {
                log.error("Failed to auto-cancel reservation: {}", reservation.getId(), e);
            }
        }

        log.info("Auto-cancelled {} pending reservations", cancelledCount);
        return cancelledCount;
    }

    // ==================== RESERVATION DEMANDS ====================

    public List<ReservationDemandResponseDto> getAllDemands() {
        return reservationDemandMapper.toDto(reservationDemandRepository.findAll());
    }

    public ReservationDemandResponseDto getDemandById(UUID id) {
        return reservationDemandMapper.toDto(findReservationDemandById(id));
    }

    @Transactional
    public ReservationDemandResponseDto createDemand(ReservationDemandRequestDto dto) {
        ReservationDemand demand = reservationDemandMapper.toEntity(dto);
        demand.setId(null);
        demand.setReservation(findReservationById(dto.getReservationId()));
        demand.setUser(findUserById(dto.getUserId()));

        return reservationDemandMapper.toDto(reservationDemandRepository.save(demand));
    }

    @Transactional
    public ReservationDemandResponseDto updateDemand(UUID id, ReservationDemandRequestDto dto) {
        ReservationDemand demand = findReservationDemandById(id);
        reservationDemandMapper.updateEntity(dto, demand);
        demand.setReservation(findReservationById(dto.getReservationId()));
        demand.setUser(findUserById(dto.getUserId()));

        return reservationDemandMapper.toDto(reservationDemandRepository.save(demand));
    }

    @Transactional
    public void deleteDemand(UUID id) {
        if (!reservationDemandRepository.existsById(id)) {
            throw new EntityNotFoundException("ReservationDemand not found with id: " + id);
        }
        reservationDemandRepository.deleteById(id);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private Reservation findReservationById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found with id: " + id));
    }

    private ReservationDemand findReservationDemandById(UUID id) {
        return reservationDemandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReservationDemand not found with id: " + id));
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }
}
