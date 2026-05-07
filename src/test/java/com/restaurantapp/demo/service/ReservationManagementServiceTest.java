package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import com.restaurantapp.demo.exception.ConflictException;
import com.restaurantapp.demo.mapper.ReservationDemandMapper;
import com.restaurantapp.demo.mapper.ReservationMapper;
import com.restaurantapp.demo.repository.ReservationDemandRepository;
import com.restaurantapp.demo.repository.ReservationRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationManagementServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationDemandRepository reservationDemandRepository;

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private UserRepository userRepository;

    private ReservationManagementService service;

    private final ReservationMapper reservationMapper = Mappers.getMapper(ReservationMapper.class);
    private final ReservationDemandMapper reservationDemandMapper = Mappers.getMapper(ReservationDemandMapper.class);

    /*@BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-30T09:00:00Z"), ZONE);
        service = new ReservationManagementService(
                reservationRepository,
                reservationDemandRepository,
                restaurantTableRepository,
                userRepository,
                reservationMapper,
                reservationDemandMapper,
                clock
        );
    }

    @Test
    void createReservation_success_selectsSmallestAvailableTablesAndCalculatesEndAt() {
        ReservationRequestDto dto = validCreateRequest();
        dto.setNumberOfPeople(5);
        dto.setStartAt(LocalDateTime.of(2026, 4, 30, 12, 30));
        dto.setDurationReservationMinutes(90);

        RestaurantTable small = table(UUID.randomUUID(), "T1", "TAB-0001", 2, true);
        RestaurantTable medium = table(UUID.randomUUID(), "T2", "TAB-0002", 3, true);
        RestaurantTable large = table(UUID.randomUUID(), "T3", "TAB-0003", 6, true);

        when(restaurantTableRepository.findActiveTablesForReservationSelection()).thenReturn(List.of(small, medium, large));
        when(reservationRepository.findOverlappingReservations(any(), any(), any())).thenReturn(List.of());
        when(reservationRepository.count()).thenReturn(0L);
        when(reservationRepository.existsByReservationCode(anyString())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ReservationResponseDto result = service.createReservation(dto);

        assertThat(result.getReservationId()).isNotNull();
        assertThat(result.getReservationCode()).isEqualTo("RSV-0001");
        assertThat(result.getStartAt()).isEqualTo(LocalDateTime.of(2026, 4, 30, 12, 30));
        assertThat(result.getEndAt()).isEqualTo(LocalDateTime.of(2026, 4, 30, 15, 0));
        assertThat(result.getSelectedTables()).extracting("tableId").containsExactly(small.getId(), medium.getId());
        assertThat(result.getSelectedTables()).extracting("publicCode").containsExactly("TAB-0001", "TAB-0002");
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createReservation_noAvailableTables_throwsConflictException() {
        ReservationRequestDto dto = validCreateRequest();
        dto.setNumberOfPeople(4);

        RestaurantTable small = table(UUID.randomUUID(), "T1", "TAB-0001", 2, true);
        RestaurantTable medium = table(UUID.randomUUID(), "T2", "TAB-0002", 4, true);
        Reservation overlapping = reservation(UUID.randomUUID());

        when(restaurantTableRepository.findActiveTablesForReservationSelection()).thenReturn(List.of(small, medium));
        when(reservationRepository.findOverlappingReservations(any(), any(), any())).thenReturn(List.of(overlapping));

        assertThatThrownBy(() -> service.createReservation(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("All restaurant tables are fully reserved at this time.");
        verify(reservationRepository, never()).save(any());
        verify(reservationRepository, never()).count();
    }

    @Test
    void createReservation_notEnoughSeats_throwsConflictException() {
        ReservationRequestDto dto = validCreateRequest();
        dto.setNumberOfPeople(7);

        RestaurantTable small = table(UUID.randomUUID(), "T1", "TAB-0001", 2, true);
        RestaurantTable medium = table(UUID.randomUUID(), "T2", "TAB-0002", 3, true);

        when(restaurantTableRepository.findActiveTablesForReservationSelection()).thenReturn(List.of(small, medium));
        when(reservationRepository.findOverlappingReservations(any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.createReservation(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Only 5 seats are available, not enough for 7 people.");
        verify(reservationRepository, never()).save(any());
        verify(reservationRepository, never()).count();
    }

    @Test
    void createReservation_pastStartTime_throwsIllegalArgumentException() {
        ReservationRequestDto dto = validCreateRequest();
        dto.setStartAt(LocalDateTime.of(2026, 4, 30, 8, 30));

        assertThatThrownBy(() -> service.createReservation(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reservation start time must be in the future.");
        verifyNoInteractions(restaurantTableRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservation_outsideOpeningHours_throwsIllegalArgumentException() {
        ReservationRequestDto dto = validCreateRequest();
        dto.setStartAt(LocalDateTime.of(2026, 4, 30, 11, 30));

        assertThatThrownBy(() -> service.createReservation(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reservation start time must be between 12:00 and 21:00.");
        verifyNoInteractions(restaurantTableRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservation_sameDayRestrictionRejectsTomorrow() {
        ReservationRequestDto dto = validCreateRequest();
        dto.setStartAt(LocalDateTime.of(2026, 5, 1, 12, 30));

        assertThatThrownBy(() -> service.createReservation(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reservation start time must be on the same day as the current date.");
        verifyNoInteractions(restaurantTableRepository);
        verifyNoInteractions(reservationRepository);
    }

    private static ReservationRequestDto validCreateRequest() {
        ReservationRequestDto dto = new ReservationRequestDto();
        dto.setNumberOfPeople(5);
        dto.setStartAt(LocalDateTime.of(2026, 4, 30, 12, 30));
        dto.setDurationReservationMinutes(90);
        dto.setCustomerName("Jane Doe");
        dto.setCustomerPhone("0612345678");
        dto.setEmailCustomer("jane@example.com");
        dto.setNotes("Window seat");
        return dto;
    }

    private static RestaurantTable table(UUID id, String label, String publicCode, Integer seats, Boolean active) {
        RestaurantTable table = new RestaurantTable();
        table.setId(id);
        table.setLabel(label);
        table.setPublicCode(publicCode);
        table.setSeats(seats);
        table.setActive(active);
        return table;
    }

    private static Reservation reservation(UUID id) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservation;
    }*/
}
