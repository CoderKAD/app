package com.restaurantapp.demo.dto.requestDto;

import com.restaurantapp.demo.entity.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateRequestDto {

    @NotNull(message = "Reservation status is required")
    private ReservationStatus status;

    @Size(max = 255, message = "Cancel reason must be at most 255 characters")
    private String cancelReason;

    private UUID updatedById;
}
