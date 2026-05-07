package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationStatusUpdateRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import com.restaurantapp.demo.entity.enums.TableStatus;
import com.restaurantapp.demo.exception.ConflictException;
import com.restaurantapp.demo.repository.ReservationRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationStatusUpdateServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private UserRepository userRepository;

    private ReservationManagementService service;

    @BeforeEach
    void setUp() {
        service = new ReservationManagementService(reservationRepository, restaurantTableRepository, userRepository);
    }

    @Test
    void updateReservation_confirmsReservationAndMarksTablesReserved() {
        UUID id = UUID.randomUUID();
        RestaurantTable table1 = table(TableStatus.Available);
        RestaurantTable table2 = table(TableStatus.Available);
        Reservation reservation = reservation(ReservationStatus.PENDING, List.of(table1, table2));
        ReservationStatusUpdateRequestDto dto = new ReservationStatusUpdateRequestDto(ReservationStatus.CONFIRMED, null, null);

        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        ReservationResponseDto result = service.updateReservation(id, dto);
        LocalDateTime after = LocalDateTime.now();

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(result.getConfirmedAt()).isBetween(before, after);
        assertThat(table1.getStatus()).isEqualTo(TableStatus.Reserved);
        assertThat(table2.getStatus()).isEqualTo(TableStatus.Reserved);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void updateReservation_cancelsReservationAndMarksTablesAvailable() {
        UUID id = UUID.randomUUID();
        RestaurantTable table = table(TableStatus.Reserved);
        Reservation reservation = reservation(ReservationStatus.CONFIRMED, List.of(table));
        ReservationStatusUpdateRequestDto dto = new ReservationStatusUpdateRequestDto(
                ReservationStatus.CANCELLED,
                "Guest requested cancellation",
                null
        );

        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        ReservationResponseDto result = service.updateReservation(id, dto);
        LocalDateTime after = LocalDateTime.now();

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.getCancelledAt()).isBetween(before, after);
        assertThat(result.getCancelReason()).isEqualTo("Guest requested cancellation");
        assertThat(table.getStatus()).isEqualTo(TableStatus.Available);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void updateReservation_completesReservationAndMarksTablesAvailable() {
        UUID id = UUID.randomUUID();
        RestaurantTable table = table(TableStatus.Reserved);
        Reservation reservation = reservation(ReservationStatus.CONFIRMED, List.of(table));
        ReservationStatusUpdateRequestDto dto = new ReservationStatusUpdateRequestDto(ReservationStatus.COMPLETED, null, null);

        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDto result = service.updateReservation(id, dto);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(table.getStatus()).isEqualTo(TableStatus.Available);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void updateReservation_invalidTransitionFromCompletedToConfirmed_throwsConflictException() {
        UUID id = UUID.randomUUID();
        Reservation reservation = reservation(ReservationStatus.COMPLETED, List.of(table(TableStatus.Available)));
        ReservationStatusUpdateRequestDto dto = new ReservationStatusUpdateRequestDto(ReservationStatus.CONFIRMED, null, null);

        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.updateReservation(id, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot change reservation status from COMPLETED to CONFIRMED.");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateReservation_missingCancelReason_throwsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        Reservation reservation = reservation(ReservationStatus.CONFIRMED, List.of(table(TableStatus.Reserved)));
        ReservationStatusUpdateRequestDto dto = new ReservationStatusUpdateRequestDto(ReservationStatus.CANCELLED, "   ", null);

        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.updateReservation(id, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cancel reason is required when cancelling a reservation.");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void getAllReservations_returnsMappedReservations() {
        Reservation reservation = reservation(ReservationStatus.PENDING, List.of(table(TableStatus.Available)));
        reservation.setReservationCode("RSV-0001");
        reservation.setNumberOfPeople(4);
        reservation.setCustomerName("John Doe");
        reservation.setCustomerPhone("0612345678");
        reservation.setEmailCustomer("john@example.com");
        reservation.setStartAt(LocalDateTime.of(2030, 1, 15, 12, 30));
        reservation.setEndAt(LocalDateTime.of(2030, 1, 15, 13, 30));
        reservation.setDurationReservationMinutes(60);

        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        List<ReservationResponseDto> result = service.getAllReservations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReservationCode()).isEqualTo("RSV-0001");
        assertThat(result.get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository).findAll();
    }

    @Test
    void deleteReservation_releasesTablesAndDeletesReservation() {
        UUID id = UUID.randomUUID();
        RestaurantTable table = table(TableStatus.Reserved);
        Reservation reservation = reservation(ReservationStatus.CONFIRMED, List.of(table));

        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        service.deleteReservation(id);

        assertThat(table.getStatus()).isEqualTo(TableStatus.Available);
        verify(restaurantTableRepository).saveAll(List.of(table));
        verify(reservationRepository).delete(reservation);
    }

    private static Reservation reservation(ReservationStatus status, List<RestaurantTable> tables) {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setStatus(status);
        reservation.setTables(tables);
        return reservation;
    }

    private static RestaurantTable table(TableStatus status) {
        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        table.setLabel("Table");
        table.setStatus(status);
        table.setActive(true);
        table.setSeats(4);
        return table;
    }
}
