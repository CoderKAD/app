package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.OrderItemResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.OrderResponseDto;
import com.restaurantapp.demo.dto.requestDto.OrderItemRequestDto;
import com.restaurantapp.demo.dto.requestDto.OrderRequestDto;
import com.restaurantapp.demo.service.OrderManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderManagementService orderManagementService;

    @Test
    void getAllOrders_returnsOrders() {
        OrderController controller = new OrderController(orderManagementService);
        OrderResponseDto response = new OrderResponseDto();

        when(orderManagementService.getAllOrders()).thenReturn(List.of(response));

        ResponseEntity<List<OrderResponseDto>> result = controller.getAllOrders();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(orderManagementService).getAllOrders();
    }

    @Test
    void getOrderById_returnsOrder() {
        OrderController controller = new OrderController(orderManagementService);
        UUID id = UUID.randomUUID();
        OrderResponseDto response = new OrderResponseDto();

        when(orderManagementService.getOrderById(id)).thenReturn(response);

        ResponseEntity<OrderResponseDto> result = controller.getOrderById(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(orderManagementService).getOrderById(id);
    }

    @Test
    void createOrder_returnsOk() {
        OrderController controller = new OrderController(orderManagementService);
        OrderRequestDto request = new OrderRequestDto();
        OrderResponseDto response = new OrderResponseDto();

        when(orderManagementService.createOrder(request)).thenReturn(response);

        ResponseEntity<OrderResponseDto> result = controller.createOrder(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(orderManagementService).createOrder(request);
    }

    @Test
    void updateOrder_returnsOk() {
        OrderController controller = new OrderController(orderManagementService);
        UUID id = UUID.randomUUID();
        OrderRequestDto request = new OrderRequestDto();
        OrderResponseDto response = new OrderResponseDto();

        when(orderManagementService.updateOrder(eq(id), any())).thenReturn(response);

        ResponseEntity<OrderResponseDto> result = controller.updateOrder(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(orderManagementService).updateOrder(id, request);
    }

    @Test
    void deleteOrder_returnsNoContent() {
        OrderController controller = new OrderController(orderManagementService);
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.deleteOrder(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(orderManagementService).deleteOrder(id);
    }

    @Test
    void getAllOrderItems_returnsItems() {
        OrderController controller = new OrderController(orderManagementService);
        OrderItemResponseDto response = new OrderItemResponseDto();

        when(orderManagementService.getAllOrderItems()).thenReturn(List.of(response));

        ResponseEntity<List<OrderItemResponseDto>> result = controller.getAllOrderItems();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(orderManagementService).getAllOrderItems();
    }

    @Test
    void getOrderItemById_returnsItem() {
        OrderController controller = new OrderController(orderManagementService);
        UUID id = UUID.randomUUID();
        OrderItemResponseDto response = new OrderItemResponseDto();

        when(orderManagementService.getOrderItemById(id)).thenReturn(response);

        ResponseEntity<OrderItemResponseDto> result = controller.getOrderItemById(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(orderManagementService).getOrderItemById(id);
    }

    @Test
    void createOrderItem_returnsOk() {
        OrderController controller = new OrderController(orderManagementService);
        OrderItemRequestDto request = new OrderItemRequestDto();
        OrderItemResponseDto response = new OrderItemResponseDto();

        when(orderManagementService.createOrderItem(request)).thenReturn(response);

        ResponseEntity<OrderItemResponseDto> result = controller.createOrderItem(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(orderManagementService).createOrderItem(request);
    }

    @Test
    void updateOrderItem_returnsOk() {
        OrderController controller = new OrderController(orderManagementService);
        UUID id = UUID.randomUUID();
        OrderItemRequestDto request = new OrderItemRequestDto();
        OrderItemResponseDto response = new OrderItemResponseDto();

        when(orderManagementService.updateOrderItem(eq(id), any())).thenReturn(response);

        ResponseEntity<OrderItemResponseDto> result = controller.updateOrderItem(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(orderManagementService).updateOrderItem(id, request);
    }

    @Test
    void deleteOrderItem_returnsNoContent() {
        OrderController controller = new OrderController(orderManagementService);
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.deleteOrderItem(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(orderManagementService).deleteOrderItem(id);
    }
}
