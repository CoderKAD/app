package com.restaurantapp.demo.dto.ResponseDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
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
public class ReservationResponseDto {
    private UUID id;
    
    private Integer numberOfPeople;
    private String reservationCode;
    private String customerName;
    private String customerPhone;
    private String emailCustomer;
    
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime startAt;
    
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime endAt;
    
    private Integer durationReservation;
    private ReservationStatus status;
    private String notes;
    
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime updatedAt;
    
    private UUID createdById;
    private UUID updatedById;
    private List<UUID> tableIds;
}
