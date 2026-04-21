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
import com.restaurantapp.demo.exception.ConflictException;
import com.restaurantapp.demo.mapper.ReservationDemandMapper;
import com.restaurantapp.demo.mapper.ReservationMapper;
import com.restaurantapp.demo.repository.ReservationDemandRepository;
import com.restaurantapp.demo.repository.ReservationRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import com.restaurantapp.demo.util.PublicCodeGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationManagementService {

    private final ReservationRepository reservationRepository;
    private final ReservationDemandRepository reservationDemandRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationDemandMapper reservationDemandMapper;

    public List<ReservationResponseDto> getAllReservations() {
        return reservationMapper.toDto(sortReservations(reservationRepository.findAll()));
    }

    public ReservationResponseDto getReservationById(UUID id) {
        return reservationMapper.toDto(findReservationById(id));
    }

    public ReservationResponseDto getReservationByCode(String code) {
        return reservationMapper.toDto(findReservationByCode(code));
    }

    public List<ReservationResponseDto> getReservationsByCustomerPhone(String phone) {
        validatePhone(phone);
        return reservationMapper.toDto(sortReservations(findReservationsByCustomerPhone(phone)));
    }

    public List<ReservationResponseDto> getUpcomingReservations(UUID userId) {
        findUserById(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getCreatedBy() != null
                        && Objects.equals(reservation.getCreatedBy().getId(), userId))
                .filter(reservation -> reservation.getStartAt() != null && !reservation.getStartAt().isBefore(now))
                .filter(reservation -> reservation.getStatus() != ReservationStatus.CANCELLED)
                .sorted(Comparator.comparing(Reservation::getStartAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return reservationMapper.toDto(reservations);
    }

    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto dto) {
        validateReservationRequest(dto);

        Reservation reservation = reservationMapper.toEntity(dto);
        reservation.setId(null);
        reservation.setReservationCode(generatePublicCode());

        applyReservationUsers(reservation, dto.getCreatedById(), dto.getUpdatedById(), true);
        reservation.setTables(loadTablesByIds(dto.getTableIds()));
        assertSelectedTablesCanHostParty(reservation.getTables(), dto.getNumberOfPeople());
        assertTablesAvailable(reservation.getTables(), dto.getStartAt(), dto.getEndAt(), null);
        applyStatusTimestamps(reservation);

        return reservationMapper.toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponseDto updateReservation(UUID id, ReservationRequestDto dto) {
        validateReservationRequest(dto);

        Reservation reservation = findReservationById(id);
        ensureReservationUpdatable(reservation);

        User existingCreatedBy = reservation.getCreatedBy();
        User existingUpdatedBy = reservation.getUpdatedBy();

        reservationMapper.updateEntity(dto, reservation);
        reservation.setCreatedBy(dto.getCreatedById() != null ? findUserById(dto.getCreatedById()) : existingCreatedBy);
        reservation.setUpdatedBy(dto.getUpdatedById() != null ? findUserById(dto.getUpdatedById()) : existingUpdatedBy);
        reservation.setTables(loadTablesByIds(dto.getTableIds()));
        assertSelectedTablesCanHostParty(reservation.getTables(), dto.getNumberOfPeople());
        assertTablesAvailable(reservation.getTables(), dto.getStartAt(), dto.getEndAt(), reservation.getId());
        applyStatusTimestamps(reservation);

        return reservationMapper.toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponseDto confirmReservation(UUID id, UUID confirmedByUserId) {
        Reservation reservation = findReservationById(id);
        ensureCanTransition(reservation, ReservationStatus.PENDING, "confirmed");

        if (confirmedByUserId != null) {
            reservation.setUpdatedBy(findUserById(confirmedByUserId));
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservation.setCancelledAt(null);
        reservation.setCancelReason(null);

        return reservationMapper.toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponseDto cancelReservation(UUID id, String reason, UUID cancelledByUserId) {
        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ConflictException("Reservation is already cancelled.");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new ConflictException("Completed reservations cannot be cancelled.");
        }

        if (cancelledByUserId != null) {
            reservation.setUpdatedBy(findUserById(cancelledByUserId));
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancelReason(reason);

        return reservationMapper.toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public void deleteReservation(UUID id) {
        Reservation reservation = findReservationById(id);
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CANCELLED) {
            throw new ConflictException("Only pending or cancelled reservations can be deleted.");
        }
        reservationRepository.delete(reservation);
    }

    public boolean isAvailable(Integer numberOfPeople, LocalDateTime startAt, LocalDateTime endAt) {
        validateAvailabilityRequest(numberOfPeople, startAt, endAt);

        List<RestaurantTable> candidateTables = restaurantTableRepository.findAll().stream()
                .filter(table -> Boolean.TRUE.equals(table.getActive()))
                .filter(table -> table.getSeats() != null && table.getSeats() > 0)
                .sorted(Comparator.comparing(RestaurantTable::getSeats, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int seated = 0;
        for (RestaurantTable table : candidateTables) {
            if (!hasConflictingReservation(table.getId(), startAt, endAt, null)) {
                seated += table.getSeats();
                if (seated >= numberOfPeople) {
                    return true;
                }
            }
        }

        return false;
    }

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
        ReservationDemand demand = findReservationDemandById(id);
        reservationDemandRepository.delete(demand);
    }

    private void validateReservationRequest(ReservationRequestDto dto) {
        if (dto.getStartAt() == null || dto.getEndAt() == null) {
            throw new IllegalArgumentException("Reservation start and end times are required.");
        }
        if (!dto.getEndAt().isAfter(dto.getStartAt())) {
            throw new IllegalArgumentException("Reservation end time must be after the start time.");
        }
        if (dto.getTableIds() == null || dto.getTableIds().isEmpty()) {
            throw new IllegalArgumentException("At least one table id is required.");
        }
        if (dto.getTableIds().size() != new HashSet<>(dto.getTableIds()).size()) {
            throw new IllegalArgumentException("tableIds must not contain duplicates.");
        }
    }

    private void validateAvailabilityRequest(Integer numberOfPeople, LocalDateTime startAt, LocalDateTime endAt) {
        if (numberOfPeople == null || numberOfPeople < 1) {
            throw new IllegalArgumentException("Number of people must be greater than zero.");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("Start and end times are required.");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Reservation end time must be after the start time.");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Customer phone is required.");
        }
    }

    private void ensureReservationUpdatable(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException("Only pending reservations can be updated.");
        }
    }

    private void ensureCanTransition(Reservation reservation, ReservationStatus expectedStatus, String actionDescription) {
        if (reservation.getStatus() != expectedStatus) {
            throw new ConflictException("Only " + expectedStatus.name().toLowerCase() + " reservations can be " + actionDescription + ".");
        }
    }

    private List<Reservation> sortReservations(List<Reservation> reservations) {
        return reservations.stream()
                .sorted(Comparator.comparing(Reservation::getStartAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Reservation findReservationById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found with id: " + id));
    }

    private Reservation findReservationByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Reservation code is required.");
        }

        return reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getReservationCode() != null
                        && reservation.getReservationCode().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found with code: " + code));
    }

    private List<Reservation> findReservationsByCustomerPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return reservationRepository.findAll().stream()
                .filter(reservation -> normalizedPhone.equals(normalizePhone(reservation.getCustomerPhone())))
                .sorted(Comparator.comparing(Reservation::getStartAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private ReservationDemand findReservationDemandById(UUID id) {
        return reservationDemandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReservationDemand not found with id: " + id));
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    private List<RestaurantTable> loadTablesByIds(List<UUID> tableIds) {
        List<RestaurantTable> tables = new ArrayList<>();
        Map<UUID, RestaurantTable> tableById = new HashMap<>();

        for (RestaurantTable table : restaurantTableRepository.findAllById(tableIds)) {
            tableById.put(table.getId(), table);
        }

        for (UUID tableId : tableIds) {
            RestaurantTable table = tableById.get(tableId);
            if (table == null) {
                throw new EntityNotFoundException("RestaurantTable not found with id: " + tableId);
            }
            if (!Boolean.TRUE.equals(table.getActive())) {
                throw new IllegalArgumentException("RestaurantTable is not active: " + tableId);
            }
            tables.add(table);
        }

        return tables;
    }

    private void assertSelectedTablesCanHostParty(List<RestaurantTable> tables, Integer numberOfPeople) {
        int seats = tables.stream()
                .map(RestaurantTable::getSeats)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        if (seats < numberOfPeople) {
            throw new ConflictException("Selected tables do not have enough seats for " + numberOfPeople + " people.");
        }
    }

    private void assertTablesAvailable(List<RestaurantTable> tables,
                                       LocalDateTime startAt,
                                       LocalDateTime endAt,
                                       UUID reservationIdToIgnore) {
        List<UUID> tableIds = tables.stream()
                .map(RestaurantTable::getId)
                .toList();

        List<Reservation> overlappingReservations = reservationRepository
                .findOverlappingReservationsForTables(tableIds, startAt, endAt);

        boolean hasConflict = overlappingReservations.stream()
                .filter(reservation -> reservation.getStatus() != ReservationStatus.CANCELLED)
                .anyMatch(reservation -> reservationIdToIgnore == null
                        || !reservationIdToIgnore.equals(reservation.getId()));

        if (hasConflict) {
            throw new ConflictException("One or more selected tables are already reserved for that time slot.");
        }
    }

    private boolean hasConflictingReservation(UUID tableId,
                                              LocalDateTime startAt,
                                              LocalDateTime endAt,
                                              UUID reservationIdToIgnore) {
        return reservationRepository.findOverlappingReservations(tableId, startAt, endAt).stream()
                .filter(reservation -> reservation.getStatus() != ReservationStatus.CANCELLED)
                .anyMatch(reservation -> reservationIdToIgnore == null
                        || !reservationIdToIgnore.equals(reservation.getId()));
    }

    private void applyReservationUsers(Reservation reservation, UUID createdById, UUID updatedById, boolean defaultUpdatedByToCreator) {
        if (createdById != null) {
            User creator = findUserById(createdById);
            reservation.setCreatedBy(creator);
            if (defaultUpdatedByToCreator && updatedById == null) {
                reservation.setUpdatedBy(creator);
            }
        }

        if (updatedById != null) {
            reservation.setUpdatedBy(findUserById(updatedById));
        }
    }

    private void applyStatusTimestamps(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CONFIRMED && reservation.getConfirmedAt() == null) {
            reservation.setConfirmedAt(LocalDateTime.now());
            reservation.setCancelledAt(null);
            reservation.setCancelReason(null);
        } else if (reservation.getStatus() == ReservationStatus.CANCELLED && reservation.getCancelledAt() == null) {
            reservation.setCancelledAt(LocalDateTime.now());
            reservation.setConfirmedAt(null);
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.replaceAll("\\D+", "");
    }

    private String generatePublicCode() {
        long nextSequence = reservationRepository.count() + 1L;
        return PublicCodeGenerator.generateReservationCode(nextSequence, reservationRepository::existsByReservationCode);
    }
}
