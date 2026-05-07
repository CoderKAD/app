package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.dto.requestDto.ReservationStatusUpdateRequestDto;
import com.restaurantapp.demo.service.ReservationManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationManagementService reservationManagementService;

    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations() {
        return ResponseEntity.ok(reservationManagementService.getAllReservations());
    }

    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(@Valid @RequestBody ReservationRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationManagementService.createReservation(dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ReservationResponseDto> updateReservation(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationStatusUpdateRequestDto dto
    ) {
        return ResponseEntity.ok(reservationManagementService.updateReservation(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable UUID id) {
        reservationManagementService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
