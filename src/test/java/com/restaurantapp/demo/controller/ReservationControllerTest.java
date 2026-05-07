package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.dto.requestDto.ReservationStatusUpdateRequestDto;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import com.restaurantapp.demo.service.ReservationManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReservationManagementService reservationManagementService;

    @Test
    void createReservation_returnsCreated() {
        ReservationController controller = new ReservationController(reservationManagementService);
        ReservationRequestDto request = new ReservationRequestDto();

        ReservationResponseDto response = new ReservationResponseDto();
        response.setReservationId(UUID.randomUUID());
        response.setReservationCode("RSV-0001");
        response.setStatus(ReservationStatus.PENDING);
        response.setStartAt(LocalDateTime.of(2030, 1, 15, 12, 30));

        when(reservationManagementService.createReservation(any())).thenReturn(response);

        ResponseEntity<ReservationResponseDto> result = controller.createReservation(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(reservationManagementService).createReservation(request);
    }

    @Test
    void updateReservation_returnsOk() {
        ReservationController controller = new ReservationController(reservationManagementService);
        UUID reservationId = UUID.randomUUID();
        ReservationStatusUpdateRequestDto request = new ReservationStatusUpdateRequestDto();
        request.setStatus(ReservationStatus.CONFIRMED);

        ReservationResponseDto response = new ReservationResponseDto();
        response.setReservationId(reservationId);
        response.setStatus(ReservationStatus.CONFIRMED);

        when(reservationManagementService.updateReservation(eq(reservationId), any())).thenReturn(response);

        ResponseEntity<ReservationResponseDto> result = controller.updateReservation(reservationId, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(reservationManagementService).updateReservation(reservationId, request);
    }

    @Test
    void getAllReservations_returnsReservations() {
        ReservationController controller = new ReservationController(reservationManagementService);
        ReservationResponseDto response = new ReservationResponseDto();
        response.setReservationId(UUID.randomUUID());
        response.setStatus(ReservationStatus.PENDING);

        when(reservationManagementService.getAllReservations()).thenReturn(List.of(response));

        ResponseEntity<List<ReservationResponseDto>> result = controller.getAllReservations();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(reservationManagementService).getAllReservations();
    }

    @Test
    void deleteReservation_returnsNoContent() {
        ReservationController controller = new ReservationController(reservationManagementService);
        UUID reservationId = UUID.randomUUID();

        ResponseEntity<Void> result = controller.deleteReservation(reservationId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(reservationManagementService).deleteReservation(reservationId);
    }
}
