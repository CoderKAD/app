package com.restaurantapp.demo.dto.ResponseDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.restaurantapp.demo.dto.ResponseDto.OrderItemResponseDto;
import com.restaurantapp.demo.entity.enums.OrderStatus;
import com.restaurantapp.demo.entity.enums.OrderType;
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
public class OrderResponseDto {
    private UUID id;
    private String publicCode;
    private OrderType typeOrder;
    private OrderStatus status;
    private String notes;
    private String deliveryAddress;
    private String phone;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private UUID restaurantTableId;
    private UUID createdById;
    private UUID updatedById;
    private List<OrderItemResponseDto> orderItems;
}