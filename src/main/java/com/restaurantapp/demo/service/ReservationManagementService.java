package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.ReservationDemandResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.ReservationSelectedTableDto;
import com.restaurantapp.demo.dto.requestDto.ReservationDemandRequestDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.dto.requestDto.ReservationStatusUpdateRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.TableStatus;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import com.restaurantapp.demo.exception.ConflictException;
import com.restaurantapp.demo.repository.ReservationRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import com.restaurantapp.demo.util.PhoneNumberUtils;
import com.restaurantapp.demo.util.PublicCodeGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationManagementService {
    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;

    private static final LocalTime OPEN_TIME = LocalTime.of(12, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(21, 0);
    private static final String MOROCCO_REGION_CODE = "MA";

    public List<ReservationResponseDto> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ReservationResponseDto createReservation(ReservationRequestDto dto) {

        // =========================
        // 0. VALIDATE PHONE NUMBER (MANDATORY)
        // =========================
        validatePhoneNumber(dto.getCustomerPhone());

        // =========================
        // 1. VALIDATE TIME
        // =========================
        LocalDateTime now = LocalDateTime.now();

        if (dto.getStartAt().isBefore(now)) {
            throw new IllegalArgumentException("Reservation must be in the future.");
        }

        LocalTime startTime = dto.getStartAt().toLocalTime();

        if (startTime.isBefore(OPEN_TIME) || startTime.isAfter(CLOSE_TIME)) {
            throw new IllegalArgumentException("Reservation must be between 12:00 and 21:00.");
        }

        // =========================
        // 2. CALCULATE END TIME
        // =========================
        LocalDateTime endAt = dto.getStartAt()
                .plusMinutes(dto.getDurationReservationMinutes())
                .plusMinutes(60); // cleaning time

        // =========================
        // 3. LOAD AVAILABLE TABLES
        // =========================
        List<RestaurantTable> allTables = tableRepository.findAll();

        List<RestaurantTable> availableTables = allTables.stream()
                .filter(RestaurantTable::getActive)
                .filter(table -> isTableAvailable(table, dto.getStartAt(), endAt))
                .sorted((a, b) -> a.getSeats().compareTo(b.getSeats())) // smallest first
                .toList();

        if (availableTables.isEmpty()) {
            throw new ConflictException("All restaurant tables are fully reserved at this time.");
        }

        // =========================
        // 4. SELECT OPTIMAL TABLES
        // =========================
        List<RestaurantTable> selectedTables = new ArrayList<>();
        int totalSeats = 0;

        for (RestaurantTable table : availableTables) {
            selectedTables.add(table);
            totalSeats += table.getSeats();

            if (totalSeats >= dto.getNumberOfPeople()) {
                break;
            }
        }

        // =========================
        // 5. CAPACITY CHECK
        // =========================
        if (totalSeats < dto.getNumberOfPeople()) {
            throw new ConflictException(
                    "Only " + totalSeats + " seats are available, not enough for " + dto.getNumberOfPeople() + " people."
            );
        }

        // =========================
        // 6. CREATE RESERVATION
        // =========================
        Reservation reservation = new Reservation();
        reservation.setReservationCode(generatePublicCode());
        reservation.setNumberOfPeople(dto.getNumberOfPeople());
        reservation.setCustomerName(dto.getCustomerName());
        reservation.setCustomerPhone(dto.getCustomerPhone()); // Store original input
        reservation.setEmailCustomer(dto.getEmailCustomer());
        reservation.setStartAt(dto.getStartAt());
        reservation.setEndAt(endAt);
        reservation.setDurationReservationMinutes(dto.getDurationReservationMinutes());
        reservation.setNotes(dto.getNotes());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setTables(selectedTables);

        if (dto.getCreatedById() != null) {
            User user = userRepository.findById(dto.getCreatedById())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            reservation.setCreatedBy(user);
        }

        Reservation saved = reservationRepository.save(reservation);

        // =========================
        // 7. BUILD RESPONSE
        // =========================
        return mapToResponse(saved);
    }

    @Transactional
    public ReservationResponseDto updateReservation(UUID reservationId, ReservationStatusUpdateRequestDto dto) {
        if (dto == null || dto.getStatus() == null) {
            throw new IllegalArgumentException("Reservation status is required.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found with id: " + reservationId));

        ReservationStatus currentStatus = reservation.getStatus();
        ReservationStatus targetStatus = dto.getStatus();

        if (currentStatus == targetStatus) {
            return mapToResponse(reservation);
        }

        validateStatusTransition(currentStatus, targetStatus);

        if (dto.getUpdatedById() != null) {
            User user = userRepository.findById(dto.getUpdatedById())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            reservation.setUpdatedBy(user);
        }

        switch (targetStatus) {
            case CONFIRMED -> {
                reservation.setStatus(ReservationStatus.CONFIRMED);
                reservation.setConfirmedAt(LocalDateTime.now());
                reservation.setCancelledAt(null);
                reservation.setCancelReason(null);
                updateTableStatuses(reservation.getTables(), TableStatus.Reserved);
            }
            case CANCELLED -> {
                if (dto.getCancelReason() == null || dto.getCancelReason().isBlank()) {
                    throw new IllegalArgumentException("Cancel reason is required when cancelling a reservation.");
                }
                reservation.setStatus(ReservationStatus.CANCELLED);
                reservation.setCancelledAt(LocalDateTime.now());
                reservation.setCancelReason(dto.getCancelReason().trim());
                updateTableStatuses(reservation.getTables(), TableStatus.Available);
            }
            case COMPLETED -> {
                reservation.setStatus(ReservationStatus.COMPLETED);
                reservation.setCancelReason(null);
                reservation.setCancelledAt(null);
                updateTableStatuses(reservation.getTables(), TableStatus.Available);
            }
            default -> throw new IllegalArgumentException("Unsupported reservation status: " + targetStatus);
        }

        return mapToResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void deleteReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found with id: " + reservationId));

        updateTableStatuses(reservation.getTables(), TableStatus.Available);
        if (reservation.getTables() != null && !reservation.getTables().isEmpty()) {
            tableRepository.saveAll(reservation.getTables());
        }

        reservationRepository.delete(reservation);
    }

    // =========================
    // PHONE NUMBER VALIDATION (MANDATORY FOR MOROCCO)
    // =========================
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is mandatory for reservation.");
        }

        String normalizedPhone = PhoneNumberUtils.normalize(phoneNumber.trim(), MOROCCO_REGION_CODE);
        // If normalize succeeds, phone is valid. Store normalized version if needed.
    }

    private void validateStatusTransition(ReservationStatus currentStatus, ReservationStatus targetStatus) {
        if (currentStatus == null) {
            throw new IllegalArgumentException("Current reservation status is required.");
        }

        boolean allowed = switch (currentStatus) {
            case PENDING -> targetStatus == ReservationStatus.CONFIRMED || targetStatus == ReservationStatus.CANCELLED;
            case CONFIRMED -> targetStatus == ReservationStatus.CANCELLED || targetStatus == ReservationStatus.COMPLETED;
            case CANCELLED, NO_SHOW, COMPLETED -> false;
        };

        if (!allowed) {
            throw new ConflictException(
                    "Cannot change reservation status from " + currentStatus + " to " + targetStatus + "."
            );
        }
    }

    private void updateTableStatuses(List<RestaurantTable> tables, TableStatus tableStatus) {
        if (tables == null) {
            return;
        }

        for (RestaurantTable table : tables) {
            if (table != null) {
                table.setStatus(tableStatus);
            }
        }
    }

    // =========================
    // TABLE AVAILABILITY CHECK
    // =========================
    private boolean isTableAvailable(RestaurantTable table,
                                     LocalDateTime startAt,
                                     LocalDateTime endAt) {

        return table.getReservations().stream()
                .noneMatch(res ->
                        res.getStatus() != ReservationStatus.CANCELLED &&
                                startAt.isBefore(res.getEndAt()) &&
                                endAt.isAfter(res.getStartAt())
                );
    }

    // =========================
    // RESPONSE MAPPER
    // =========================
    private ReservationResponseDto mapToResponse(Reservation r) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setReservationId(r.getId());
        dto.setReservationCode(r.getReservationCode());
        dto.setNumberOfPeople(r.getNumberOfPeople());
        dto.setCustomerName(r.getCustomerName());

        // Return normalized Moroccan phone number in response
        if (r.getCustomerPhone() != null) {
            dto.setCustomerPhone(PhoneNumberUtils.normalize(r.getCustomerPhone(), MOROCCO_REGION_CODE));
        }

        dto.setEmailCustomer(r.getEmailCustomer());
        dto.setStartAt(r.getStartAt());
        dto.setEndAt(r.getEndAt());
        dto.setDurationReservationMinutes(r.getDurationReservationMinutes());
        dto.setStatus(r.getStatus());
        dto.setNotes(r.getNotes());
        dto.setConfirmedAt(r.getConfirmedAt());
        dto.setCancelledAt(r.getCancelledAt());
        dto.setCancelReason(r.getCancelReason());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        dto.setCreatedById(r.getCreatedBy() != null ? r.getCreatedBy().getId() : null);
        dto.setUpdatedById(r.getUpdatedBy() != null ? r.getUpdatedBy().getId() : null);

        if (r.getTables() != null) {
            List<ReservationSelectedTableDto> tables = r.getTables().stream()
                    .map(t -> new ReservationSelectedTableDto(
                            t.getId(),
                            t.getPublicCode()
                    ))
                    .toList();

            dto.setSelectedTables(tables);
        }

        return dto;
    }

    // =========================
    // PUBLIC CODE GENERATOR
    // =========================
    private String generatePublicCode() {
        long nextSequence = reservationRepository.count() + 1L;
        return PublicCodeGenerator.generateReservationCode(
                nextSequence,
                reservationRepository::existsByReservationCode
        );
    }
}
