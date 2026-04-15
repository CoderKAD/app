package com.restaurantapp.demo.dto.requestDto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.restaurantapp.demo.entity.enums.DemandStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDemandRequestDto {
    @NotNull(message = "Status is required")
    private DemandStatus status;

    @NotNull(message = "Reservation id is required")
    private UUID reservationId;

    @NotNull(message = "User id is required")
    @JsonAlias("customerId")
    private UUID userId;
}
