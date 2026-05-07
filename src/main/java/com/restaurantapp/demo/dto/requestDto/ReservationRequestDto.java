package com.restaurantapp.demo.dto.requestDto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequestDto {

    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "At least 1 person is required")
    @Max(value = 50, message = "Maximum 50 people allowed")
    private Integer numberOfPeople;

    @NotNull(message = "Reservation start datetime is required")
    @Future(message = "Reservation start must be in the future")
    private LocalDateTime startAt;

    @NotNull(message = "Duration is required")
    @JsonAlias("durationMinutes")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationReservationMinutes = 60;

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 50, message = "Customer name must be between 2 and 50 characters")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    @Size(min = 8, max = 20, message = "Customer phone must be between 8 and 20 characters")
    private String customerPhone;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email must be valid")
    private String emailCustomer;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;

    private UUID createdById;
    private UUID updatedById;
}
