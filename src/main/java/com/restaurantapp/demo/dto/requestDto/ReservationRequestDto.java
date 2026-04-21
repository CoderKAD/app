package com.restaurantapp.demo.dto.requestDto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
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

    @NotNull(message = "Reservation end datetime is required")
    private LocalDateTime endAt;

    @JsonIgnore
    @AssertTrue(message = "Reservation end time must be after the start time")
    public boolean isEndAtAfterStartAt() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes = 60;

    @Min(value = 0, message = "Buffer time must be zero or greater")
    private Integer bufferTimeMinutes = 30;

    @NotNull(message = "Reservation status is required")
    private ReservationStatus status = ReservationStatus.PENDING;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 50, message = "Customer name must be between 2 and 50 characters")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    @Size(min = 8, max = 20, message = "Customer phone must be between 8 and 20 characters")
    private String customerPhone;

    @Email(message = "Customer email must be valid")
    private String emailCustomer;

    private UUID createdById;
    private UUID updatedById;

    @NotEmpty(message = "At least one table id is required")
    private List<UUID> tableIds;
}
