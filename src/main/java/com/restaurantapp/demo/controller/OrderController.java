package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.OrderItemResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.OrderResponseDto;
import com.restaurantapp.demo.dto.requestDto.OrderItemRequestDto;
import com.restaurantapp.demo.dto.requestDto.OrderRequestDto;
import com.restaurantapp.demo.service.OrderManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderManagementService orderManagementService;

    public OrderController(OrderManagementService orderManagementService) {
        this.orderManagementService = orderManagementService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        return ResponseEntity.ok(orderManagementService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderManagementService.getOrderById(id));
    }



    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto dto) {
        return ResponseEntity.ok(orderManagementService.createOrder(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDto> updateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody OrderRequestDto dto
    ) {
        return ResponseEntity.ok(orderManagementService.updateOrder(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        orderManagementService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items")
    public ResponseEntity<List<OrderItemResponseDto>> getAllOrderItems() {
        return ResponseEntity.ok(orderManagementService.getAllOrderItems());
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<OrderItemResponseDto> getOrderItemById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderManagementService.getOrderItemById(id));
    }

    @PostMapping("/items")
    public ResponseEntity<OrderItemResponseDto> createOrderItem(@Valid @RequestBody OrderItemRequestDto dto) {
        return ResponseEntity.ok(orderManagementService.createOrderItem(dto));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<OrderItemResponseDto> updateOrderItem(
            @PathVariable UUID id,
            @Valid @RequestBody OrderItemRequestDto dto
    ) {
        return ResponseEntity.ok(orderManagementService.updateOrderItem(id, dto));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteOrderItem(@PathVariable UUID id) {
        orderManagementService.deleteOrderItem(id);
        return ResponseEntity.noContent().build();
    }

}