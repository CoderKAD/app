package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.ReservationDemandResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationDemandRequestDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.ReservationDemand;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.DemandStatus;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.entity.enums.TableStatus;
import com.restaurantapp.demo.exception.ConflictException;
import com.restaurantapp.demo.mapper.ReservationDemandMapper;
import com.restaurantapp.demo.mapper.ReservationMapper;
import com.restaurantapp.demo.repository.ReservationDemandRepository;
import com.restaurantapp.demo.repository.ReservationRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationManagementServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationDemandRepository reservationDemandRepository;

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ReservationDemandMapper reservationDemandMapper;

    @InjectMocks
    private ReservationManagementService reservationManagementService;

    @Test
    void getAllReservations_returnsMappedReservations() {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setReservationCode("RSV-0001");
        reservation.setNumberOfPeople(2);
        reservation.setCustomerName("Jane");
        reservation.setCustomerPhone("0612345678");
        reservation.setEmailCustomer("jane@example.com");
        reservation.setStartAt(LocalDateTime.of(2026, 5, 1, 18, 0));
        reservation.setEndAt(LocalDateTime.of(2026, 5, 1, 20, 0));
        reservation.setDurationMinutes(120);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setBufferTimeMinutes(30);

        ReservationResponseDto response = new ReservationResponseDto();
        response.setId(reservation.getId());
        response.setReservationCode(reservation.getReservationCode());

        when(reservationRepository.findAll()).thenReturn(List.of(reservation));
        when(reservationMapper.toDto(List.of(reservation))).thenReturn(List.of(response));

        List<ReservationResponseDto> result = reservationManagementService.getAllReservations();

        assertThat(result).containsExactly(response);
    }

    @Test
    void createReservation_success_generatesCodeAndSavesTables() {
        UUID tableId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 1, 18, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 5, 1, 20, 0);
        RestaurantTable table = new RestaurantTable();
        table.setId(tableId);
        table.setLabel("T1");
        table.setSeats(4);
        table.setPublicCode("TAB-0001");
        table.setActive(true);
        table.setStatus(TableStatus.Available);

        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        user.setPasswordHash("hash");
        user.setEmail("john@example.com");
        user.setRoles(Role.CUSTOMER);
        ReservationRequestDto dto = new ReservationRequestDto();
        dto.setNumberOfPeople(2);
        dto.setStartAt(startAt);
        dto.setEndAt(endAt);
        dto.setDurationMinutes(60);
        dto.setBufferTimeMinutes(30);
        dto.setStatus(ReservationStatus.PENDING);
        dto.setNotes("notes");
        dto.setCustomerName("Jane");
        dto.setCustomerPhone("0612345678");
        dto.setEmailCustomer("jane@example.com");
        dto.setCreatedById(userId);
        dto.setUpdatedById(null);
        dto.setTableIds(List.of(tableId));

        Reservation entity = new Reservation();
        entity.setStatus(ReservationStatus.PENDING);
        Reservation saved = new Reservation();
        saved.setId(UUID.randomUUID());
        saved.setReservationCode("RSV-0001");
        saved.setNumberOfPeople(2);
        saved.setCustomerName("Jane");
        saved.setCustomerPhone("0612345678");
        saved.setEmailCustomer("jane@example.com");
        saved.setStartAt(startAt);
        saved.setEndAt(endAt);
        saved.setDurationMinutes(60);
        saved.setStatus(ReservationStatus.PENDING);
        saved.setNotes("notes");
        saved.setBufferTimeMinutes(30);
        saved.setCreatedBy(user);
        saved.setUpdatedBy(user);
        saved.setTables(List.of(table));

        ReservationResponseDto expected = new ReservationResponseDto();
        expected.setId(saved.getId());
        expected.setReservationCode(saved.getReservationCode());
        expected.setCreatedById(userId);
        expected.setUpdatedById(userId);
        expected.setTableIds(List.of(tableId));

        when(reservationMapper.toEntity(dto)).thenReturn(entity);
        when(restaurantTableRepository.findAllById(List.of(tableId))).thenReturn(List.of(table));
        when(reservationRepository.findOverlappingReservationsForTables(List.of(tableId), startAt, endAt)).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reservationRepository.count()).thenReturn(0L);
        when(reservationRepository.existsByReservationCode("RSV-0001")).thenReturn(false);
        when(reservationRepository.save(entity)).thenReturn(saved);
        when(reservationMapper.toDto(saved)).thenReturn(expected);

        ReservationResponseDto result = reservationManagementService.createReservation(dto);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getReservationCode()).isEqualTo("RSV-0001");
        assertThat(captor.getValue().getTables()).containsExactly(table);
        assertThat(captor.getValue().getCreatedBy()).isSameAs(user);
    }

    @Test
    void confirmReservation_success_setsConfirmedState() {
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setReservationCode("RSV-0001");
        reservation.setStatus(ReservationStatus.PENDING);

        Reservation saved = new Reservation();
        saved.setId(id);
        saved.setReservationCode("RSV-0001");
        saved.setStatus(ReservationStatus.CONFIRMED);

        ReservationResponseDto expected = new ReservationResponseDto();
        expected.setId(id);
        expected.setReservationCode("RSV-0001");
        expected.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(saved);
        when(reservationMapper.toDto(saved)).thenReturn(expected);

        ReservationResponseDto result = reservationManagementService.confirmReservation(id, null);

        assertThat(result).isSameAs(expected);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getConfirmedAt()).isNotNull();
        assertThat(reservation.getCancelledAt()).isNull();
        assertThat(reservation.getCancelReason()).isNull();
    }

    @Test
    void createDemand_success_linksReservationAndUser() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        user.setPasswordHash("hash");
        user.setEmail("john@example.com");
        user.setRoles(Role.CUSTOMER);
        ReservationDemandRequestDto dto = new ReservationDemandRequestDto(DemandStatus.PENDING, reservationId, userId);
        ReservationDemand entity = new ReservationDemand();
        ReservationDemand saved = new ReservationDemand(UUID.randomUUID(), DemandStatus.PENDING, null, null, reservation, user);
        ReservationDemandResponseDto expected = new ReservationDemandResponseDto(saved.getId(), saved.getStatus(), null, null, reservationId, userId);

        when(reservationDemandMapper.toEntity(dto)).thenReturn(entity);
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reservationDemandRepository.save(entity)).thenReturn(saved);
        when(reservationDemandMapper.toDto(saved)).thenReturn(expected);

        ReservationDemandResponseDto result = reservationManagementService.createDemand(dto);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<ReservationDemand> captor = ArgumentCaptor.forClass(ReservationDemand.class);
        verify(reservationDemandRepository).save(captor.capture());
        assertThat(captor.getValue().getReservation()).isSameAs(reservation);
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void deleteDemand_success_removesDemand() {
        UUID id = UUID.randomUUID();
        ReservationDemand demand = new ReservationDemand(id, DemandStatus.PENDING, null, null, null, null);

        when(reservationDemandRepository.findById(id)).thenReturn(Optional.of(demand));

        reservationManagementService.deleteDemand(id);

        verify(reservationDemandRepository).delete(demand);
    }

    @Test
    void deleteReservation_whenCompleted_throwsConflict() {
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStatus(ReservationStatus.COMPLETED);

        when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationManagementService.deleteReservation(id))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only pending or cancelled reservations can be deleted");
    }
}
