package com.restaurantapp.demo.dto.requestDto;

import com.restaurantapp.demo.entity.enums.OrderStatus;
import com.restaurantapp.demo.entity.enums.OrderType;
import com.restaurantapp.demo.entity.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
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
public class OrderRequestDto {
    @NotNull(message = "Order type is required")
    private OrderType typeOrder;

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    private PaymentStatus paymentStatus;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;

    @Size(max = 1000, message = "Delivery address must be at most 1000 characters")
    private String deliveryAddress;

    private UUID restaurantTableId;
    private UUID createdById;
    private UUID updatedById;


    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;


}