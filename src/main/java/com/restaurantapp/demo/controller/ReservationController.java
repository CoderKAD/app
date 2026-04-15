package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.ReservationDemandResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationDemandRequestDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.service.ReservationManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Reservation Management
 * Provides endpoints for:
 * - Creating, reading, updating, deleting reservations
 * - Confirming and cancelling reservations
 * - Checking availability
 * - Managing reservation demands
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationManagementService reservationManagementService;

    // ==================== RESERVATION RETRIEVAL ====================

    /**
     * GET /api/reservations
     * Retrieves all reservations
     */
    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations() {
        return ResponseEntity.ok(reservationManagementService.getAllReservations());
    }

    /**
     * GET /api/reservations/{id}
     * Retrieves a reservation by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDto> getReservationById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationManagementService.getReservationById(id));
    }

    /**
     * GET /api/reservations/code/{code}
     * Retrieves a reservation by its code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ReservationResponseDto> getReservationByCode(@PathVariable String code) {
        return ResponseEntity.ok(reservationManagementService.getReservationByCode(code));
    }

    /**
     * GET /api/reservations/customer/{phone}
     * Retrieves all reservations for a customer phone
     */
    @GetMapping("/customer/{phone}")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsByCustomerPhone(@PathVariable String phone) {
        return ResponseEntity.ok(reservationManagementService.getReservationsByCustomerPhone(phone));
    }

    /**
     * GET /api/reservations/user/{userId}/upcoming
     * Retrieves upcoming reservations for a user
     */
    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<List<ReservationResponseDto>> getUpcomingReservations(@PathVariable UUID userId) {
        return ResponseEntity.ok(reservationManagementService.getUpcomingReservations(userId));
    }

    // ==================== RESERVATION CREATION & MANAGEMENT ====================

    /**
     * POST /api/reservations
     * Creates a new reservation
     * - Validates availability
     * - Selects optimal tables using greedy algorithm
     * - Generates reservation code
     */
    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(@Valid @RequestBody ReservationRequestDto dto) {
        ReservationResponseDto created = reservationManagementService.createReservation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/reservations/{id}
     * Updates an existing reservation (only PENDING)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponseDto> updateReservation(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationRequestDto dto) {
        return ResponseEntity.ok(reservationManagementService.updateReservation(id, dto));
    }

    /**
     * POST /api/reservations/{id}/confirm
     * Confirms a PENDING reservation
     * Uses pessimistic locking to prevent race conditions
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponseDto> confirmReservation(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID confirmedByUserId) {
        return ResponseEntity.ok(reservationManagementService.confirmReservation(id, confirmedByUserId));
    }

    /**
     * POST /api/reservations/{id}/cancel
     * Cancels a reservation with optional reason
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponseDto> cancelReservation(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) UUID cancelledByUserId) {
        return ResponseEntity.ok(
                reservationManagementService.cancelReservation(id, reason, cancelledByUserId));
    }

    /**
     * DELETE /api/reservations/{id}
     * Deletes a reservation (only PENDING or CANCELLED)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable UUID id) {
        reservationManagementService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== AVAILABILITY CHECKING ====================

    /**
     * POST /api/reservations/check-availability
     * Checks if tables are available for the requested time slot
     */
    @PostMapping("/check-availability")
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam Integer numberOfPeople,
            @RequestParam(name = "startAt") String startAtStr,
            @RequestParam(name = "endAt") String endAtStr) {
        // Parse LocalDateTime from string (format: yyyy-MM-dd'T'HH:mm)
        java.time.LocalDateTime startAt = java.time.LocalDateTime.parse(startAtStr);
        java.time.LocalDateTime endAt = java.time.LocalDateTime.parse(endAtStr);
        
        boolean available = reservationManagementService.isAvailable(numberOfPeople, startAt, endAt);
        return ResponseEntity.ok(available);
    }

    // ==================== RESERVATION DEMANDS ====================

    /**
     * GET /api/reservations/demands
     * Retrieves all reservation demands
     */
    @GetMapping("/demands")
    public ResponseEntity<List<ReservationDemandResponseDto>> getAllDemands() {
        return ResponseEntity.ok(reservationManagementService.getAllDemands());
    }

    /**
     * GET /api/reservations/demands/{id}
     * Retrieves a demand by ID
     */
    @GetMapping("/demands/{id}")
    public ResponseEntity<ReservationDemandResponseDto> getDemandById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationManagementService.getDemandById(id));
    }

    /**
     * POST /api/reservations/demands
     * Creates a new reservation demand
     */
    @PostMapping("/demands")
    public ResponseEntity<ReservationDemandResponseDto> createDemand(
            @Valid @RequestBody ReservationDemandRequestDto dto) {
        ReservationDemandResponseDto created = reservationManagementService.createDemand(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/reservations/demands/{id}
     * Updates a demand
     */
    @PutMapping("/demands/{id}")
    public ResponseEntity<ReservationDemandResponseDto> updateDemand(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationDemandRequestDto dto) {
        return ResponseEntity.ok(reservationManagementService.updateDemand(id, dto));
    }

    /**
     * DELETE /api/reservations/demands/{id}
     * Deletes a demand
     */
    @DeleteMapping("/demands/{id}")
    public ResponseEntity<Void> deleteDemand(@PathVariable UUID id) {
        reservationManagementService.deleteDemand(id);
        return ResponseEntity.noContent().build();
    }
}