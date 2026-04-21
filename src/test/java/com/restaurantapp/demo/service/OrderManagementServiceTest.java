package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.OrderItemResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.OrderResponseDto;
import com.restaurantapp.demo.dto.requestDto.OrderItemRequestDto;
import com.restaurantapp.demo.dto.requestDto.OrderRequestDto;
import com.restaurantapp.demo.entity.MenuItem;
import com.restaurantapp.demo.entity.Order;
import com.restaurantapp.demo.entity.OrderItem;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.OrderStatus;
import com.restaurantapp.demo.entity.enums.OrderType;
import com.restaurantapp.demo.entity.enums.PaymentStatus;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.entity.enums.TableStatus;
import com.restaurantapp.demo.mapper.OrderItemMapper;
import com.restaurantapp.demo.mapper.OrderMapper;
import com.restaurantapp.demo.repository.MenuItemRepository;
import com.restaurantapp.demo.repository.OrderItemRepository;
import com.restaurantapp.demo.repository.OrderRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderManagementServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderManagementService orderManagementService;

    @Test
    void getAllOrders_returnsMappedOrders() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setPublicCode("ORD-0001");
        order.setTypeOrder(OrderType.DINE_IN);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        OrderResponseDto response = new OrderResponseDto(order.getId(), order.getPublicCode(), order.getTypeOrder(), order.getStatus(), null, null, null, null, null, null, null, null, null);

        when(orderRepository.findAllBy()).thenReturn(List.of(order));
        when(orderMapper.toDto(List.of(order))).thenReturn(List.of(response));

        List<OrderResponseDto> result = orderManagementService.getAllOrders();

        assertThat(result).containsExactly(response);
    }

    @Test
    void createOrder_dineIn_success_generatesPublicCode() {
        UUID tableId = UUID.randomUUID();
        RestaurantTable table = new RestaurantTable();
        table.setId(tableId);
        table.setLabel("T1");
        table.setSeats(4);
        table.setPublicCode("TAB-0001");
        table.setActive(true);
        table.setStatus(TableStatus.Available);
        OrderRequestDto dto = new OrderRequestDto(OrderType.DINE_IN, OrderStatus.PENDING, PaymentStatus.PENDING, "notes", null, tableId, null, null, null);
        Order order = new Order();
        order.setTypeOrder(OrderType.DINE_IN);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setNotes("notes");
        Order saved = new Order();
        saved.setId(UUID.randomUUID());
        saved.setPublicCode("ORD-0001");
        saved.setTypeOrder(OrderType.DINE_IN);
        saved.setStatus(OrderStatus.PENDING);
        saved.setPaymentStatus(PaymentStatus.PENDING);
        saved.setNotes("notes");
        saved.setRestaurantTable(table);
        OrderResponseDto expected = new OrderResponseDto(saved.getId(), saved.getPublicCode(), saved.getTypeOrder(), saved.getStatus(), saved.getNotes(), saved.getDeliveryAddress(), saved.getPhone(), null, null, tableId, null, null, null);

        when(restaurantTableRepository.findById(tableId)).thenReturn(Optional.of(table));
        when(orderMapper.toEntity(dto)).thenReturn(order);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.existsByPublicCode("ORD-0001")).thenReturn(false);
        when(orderRepository.save(order)).thenReturn(saved);
        when(orderMapper.toDto(saved)).thenReturn(expected);

        OrderResponseDto result = orderManagementService.createOrder(dto);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getPublicCode()).isEqualTo("ORD-0001");
        assertThat(captor.getValue().getRestaurantTable()).isSameAs(table);
    }

    @Test
    void createOrder_delivery_missingPhone_throws() {
        OrderRequestDto dto = new OrderRequestDto(OrderType.DELIVERY, OrderStatus.PENDING, PaymentStatus.PENDING, null, "Casablanca", null, null, null, "");

        assertThatThrownBy(() -> orderManagementService.createOrder(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number is required");
    }

    @Test
    void createOrderItem_success_linksOrderAndMenuItem() {
        UUID orderId = UUID.randomUUID();
        UUID menuItemId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        MenuItem menuItem = new MenuItem();
        menuItem.setId(menuItemId);
        menuItem.setName("Pizza");
        OrderItemRequestDto dto = new OrderItemRequestDto(2, "extra cheese", orderId, menuItemId);
        OrderItem entity = new OrderItem();
        OrderItem saved = new OrderItem();
        saved.setId(UUID.randomUUID());
        saved.setQuantity(2);
        saved.setNotes("extra cheese");
        saved.setOrder(order);
        saved.setMenuItem(menuItem);
        OrderItemResponseDto expected = new OrderItemResponseDto(saved.getId(), saved.getQuantity(), saved.getNotes(), null, null, orderId, menuItemId, menuItem.getName());

        when(orderItemMapper.toEntity(dto)).thenReturn(entity);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        when(orderItemRepository.save(entity)).thenReturn(saved);
        when(orderItemMapper.toDto(saved)).thenReturn(expected);

        OrderItemResponseDto result = orderManagementService.createOrderItem(dto);

        assertThat(result).isSameAs(expected);
        assertThat(entity.getOrder()).isSameAs(order);
        assertThat(entity.getMenuItem()).isSameAs(menuItem);
    }

    @Test
    void deleteOrder_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(orderRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> orderManagementService.deleteOrder(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository, never()).deleteById(any());
    }
}
