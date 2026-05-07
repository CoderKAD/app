package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.OrderItemResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.OrderResponseDto;
import com.restaurantapp.demo.dto.requestDto.OrderItemRequestDto;
import com.restaurantapp.demo.dto.requestDto.OrderRequestDto;
import com.restaurantapp.demo.entity.CategoryMenu;
import com.restaurantapp.demo.entity.MenuItem;
import com.restaurantapp.demo.entity.Order;
import com.restaurantapp.demo.entity.OrderItem;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.OrderStatus;
import com.restaurantapp.demo.entity.enums.OrderType;
import com.restaurantapp.demo.entity.enums.PaymentStatus;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.exception.BadRequestException;
import com.restaurantapp.demo.mapper.OrderItemMapper;
import com.restaurantapp.demo.mapper.OrderMapper;
import com.restaurantapp.demo.repository.MenuItemRepository;
import com.restaurantapp.demo.repository.OrderItemRepository;
import com.restaurantapp.demo.repository.OrderRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private OrderManagementService service;

    private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);
    private final OrderItemMapper orderItemMapper = Mappers.getMapper(OrderItemMapper.class);

    @BeforeEach
    void setUp() {
        injectNestedMapper(orderMapper, orderItemMapper);
        // Arrange the service with real mappers and mocked repositories.
        service = new OrderManagementService(
                orderRepository,
                orderItemRepository,
                restaurantTableRepository,
                userRepository,
                menuItemRepository,
                orderMapper,
                orderItemMapper
        );
    }

    @Test
    void getAllOrders_returnsMappedOrders() {
        // Arrange: one order with one order item so the mapping includes nested data.
        Order order = orderWithItems();
        when(orderRepository.findAllBy()).thenReturn(List.of(order));

        // Act: fetch all orders.
        List<OrderResponseDto> result = service.getAllOrders();

        // Assert: the DTO contains the expected nested order-item data.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPublicCode()).isEqualTo("ORD-0001");
        assertThat(result.get(0).getOrderItems()).hasSize(1);
        verify(orderRepository).findAllBy();
    }

    @Test
    void getOrderById_returnsMappedOrder() {
        // Arrange: load a single order by id.
        Order order = orderWithItems();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        // Act: fetch the order.
        OrderResponseDto result = service.getOrderById(order.getId());

        // Assert: the service maps the entity to the response DTO.
        assertThat(result.getId()).isEqualTo(order.getId());
        assertThat(result.getPublicCode()).isEqualTo("ORD-0001");
    }

    @Test
    void createOrder_dineIn_success_setsDefaultsAndCreator() {
        // Arrange: a valid dine-in order with an active table and creator user.
        UUID tableId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        RestaurantTable table = table(tableId, "Table 1", "TAB-0001", true);
        User creator = user(creatorId, "john.doe", "john.doe@test.local");
        OrderRequestDto dto = new OrderRequestDto(
                OrderType.DINE_IN,
                OrderStatus.CONFIRMED,
                PaymentStatus.PAID,
                "No salt",
                null,
                tableId,
                creatorId,
                null,
                null
        );
        when(restaurantTableRepository.findById(tableId)).thenReturn(Optional.of(table));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: create the order.
        OrderResponseDto result = service.createOrder(dto);

        // Assert: the service enforces pending defaults and assigns the creator.
        assertThat(result.getPublicCode()).isEqualTo("ORD-0001");
        assertThat(result.getTypeOrder()).isEqualTo(OrderType.DINE_IN);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getCreatedById()).isEqualTo(creatorId);
        assertThat(result.getUpdatedById()).isEqualTo(creatorId);
        assertThat(result.getRestaurantTableId()).isEqualTo(tableId);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_delivery_success_normalizesPhone() {
        // Arrange: a valid delivery order with a Moroccan phone number and address.
        UUID creatorId = UUID.randomUUID();
        User creator = user(creatorId, "john.doe", "john.doe@test.local");
        OrderRequestDto dto = new OrderRequestDto(
                OrderType.DELIVERY,
                OrderStatus.CONFIRMED,
                PaymentStatus.PAID,
                "Leave at the door",
                "Casablanca, Morocco",
                null,
                creatorId,
                null,
                "0612345678"
        );
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: create the order.
        OrderResponseDto result = service.createOrder(dto);

        // Assert: the phone number is normalized and persisted in the response.
        assertThat(result.getPhone()).startsWith("+212");
        assertThat(result.getDeliveryAddress()).isEqualTo("Casablanca, Morocco");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_missingCreatedBy_throwsBadRequestException() {
        // Arrange: the request omits createdById.
        OrderRequestDto dto = new OrderRequestDto(
                OrderType.DELIVERY,
                OrderStatus.PENDING,
                PaymentStatus.PENDING,
                "Notes",
                "Address",
                null,
                null,
                null,
                "0612345678"
        );

        // Act and Assert: createdById is mandatory for order creation.
        assertThatThrownBy(() -> service.createOrder(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("createdById is required for order creation");
        verifyNoInteractions(userRepository);
    }

    @Test
    void createOrder_dineIn_missingTable_throwsBadRequestException() {
        // Arrange: dine-in orders require a table id.
        UUID creatorId = UUID.randomUUID();
        User creator = user(creatorId, "john.doe", "john.doe@test.local");
        OrderRequestDto dto = new OrderRequestDto(
                OrderType.DINE_IN,
                OrderStatus.PENDING,
                PaymentStatus.PENDING,
                "Notes",
                null,
                null,
                creatorId,
                null,
                null
        );

        // Act and Assert: the service rejects dine-in orders without a table.
        assertThatThrownBy(() -> service.createOrder(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("restaurantTableId is required for dine-in orders");
        verifyNoInteractions(restaurantTableRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_delivery_missingPhone_throwsBadRequestException() {
        // Arrange: delivery orders must include a phone number.
        UUID creatorId = UUID.randomUUID();
        User creator = user(creatorId, "john.doe", "john.doe@test.local");
        OrderRequestDto dto = new OrderRequestDto(
                OrderType.DELIVERY,
                OrderStatus.PENDING,
                PaymentStatus.PENDING,
                "Notes",
                "Address",
                null,
                creatorId,
                null,
                null
        );

        // Act and Assert: the service rejects missing phone values for delivery.
        assertThatThrownBy(() -> service.createOrder(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Phone number is required for delivery orders");
    }

    @Test
    void createOrder_delivery_missingAddress_throwsBadRequestException() {
        // Arrange: delivery orders must include an address.
        UUID creatorId = UUID.randomUUID();
        User creator = user(creatorId, "john.doe", "john.doe@test.local");
        OrderRequestDto dto = new OrderRequestDto(
                OrderType.DELIVERY,
                OrderStatus.PENDING,
                PaymentStatus.PENDING,
                "Notes",
                null,
                null,
                creatorId,
                null,
                "0612345678"
        );

        // Act and Assert: the service rejects missing delivery addresses.
        assertThatThrownBy(() -> service.createOrder(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Delivery address is required for delivery orders");
    }

    @Test
    void updateOrder_success_keepsExistingRelationsWhenIdsAreMissing() {
        // Arrange: an existing order with a table and creator, then update only notes.
        UUID orderId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID updaterId = UUID.randomUUID();
        RestaurantTable table = table(tableId, "Table 1", "TAB-0001", true);
        User creator = user(creatorId, "creator", "creator@test.local");
        User updater = user(updaterId, "updater", "updater@test.local");
        Order existing = order(orderId, "ORD-0001", OrderType.DINE_IN, OrderStatus.PENDING, PaymentStatus.PENDING, "Old notes", null, null, table, creator, updater);
        OrderRequestDto dto = new OrderRequestDto(
                OrderType.DINE_IN,
                OrderStatus.PENDING,
                PaymentStatus.PENDING,
                "New notes",
                null,
                null,
                null,
                null,
                null
        );
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: update the order with only the notes field.
        OrderResponseDto result = service.updateOrder(orderId, dto);

        // Assert: the service preserves the existing associations and updates notes only.
        assertThat(result.getNotes()).isEqualTo("New notes");
        assertThat(result.getRestaurantTableId()).isEqualTo(tableId);
        assertThat(result.getCreatedById()).isEqualTo(creatorId);
        assertThat(result.getUpdatedById()).isEqualTo(updaterId);
    }

    @Test
    void deleteOrder_success_deletesOrder() {
        // Arrange: an existing order is returned by the repository.
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(orderId, "ORD-0001", OrderType.DINE_IN, OrderStatus.PENDING, PaymentStatus.PENDING, null, null, null, null, null, null)));

        // Act: delete the order.
        service.deleteOrder(orderId);

        // Assert: the repository delete method is called with the loaded order.
        verify(orderRepository).delete(any(Order.class));
    }

    @Test
    void createOrderItem_success_persistsItem() {
        // Arrange: an active menu item and an open order.
        UUID orderId = UUID.randomUUID();
        UUID menuItemId = UUID.randomUUID();
        Order order = order(orderId, "ORD-0001", OrderType.DINE_IN, OrderStatus.PENDING, PaymentStatus.PENDING, null, null, null, null, null, null);
        MenuItem menuItem = menuItem(menuItemId, "Pizza", true);
        OrderItemRequestDto dto = new OrderItemRequestDto(2, "Extra cheese", orderId, menuItemId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: create the order item.
        OrderItemResponseDto result = service.createOrderItem(dto);

        // Assert: the item is saved and mapped back to the response DTO.
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getMenuItemName()).isEqualTo("Pizza");
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void createOrderItem_inactiveMenuItem_throwsBadRequestException() {
        // Arrange: the menu item exists but is not active.
        UUID orderId = UUID.randomUUID();
        UUID menuItemId = UUID.randomUUID();
        Order order = order(orderId, "ORD-0001", OrderType.DINE_IN, OrderStatus.PENDING, PaymentStatus.PENDING, null, null, null, null, null, null);
        MenuItem menuItem = menuItem(menuItemId, "Pizza", false);
        OrderItemRequestDto dto = new OrderItemRequestDto(2, "Extra cheese", orderId, menuItemId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        // Act and Assert: inactive menu items cannot be added to orders.
        assertThatThrownBy(() -> service.createOrderItem(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Menu item is not available: Pizza");
    }

    @Test
    void createOrderItem_completedOrder_throwsBadRequestException() {
        // Arrange: the order is already completed.
        UUID orderId = UUID.randomUUID();
        UUID menuItemId = UUID.randomUUID();
        Order order = order(orderId, "ORD-0001", OrderType.DINE_IN, OrderStatus.COMPLETED, PaymentStatus.PENDING, null, null, null, null, null, null);
        MenuItem menuItem = menuItem(menuItemId, "Pizza", true);
        OrderItemRequestDto dto = new OrderItemRequestDto(2, "Extra cheese", orderId, menuItemId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        // Act and Assert: completed orders cannot receive more items.
        assertThatThrownBy(() -> service.createOrderItem(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot add items to completed or cancelled orders");
    }

    @Test
    void updateOrderItem_success_reassignsRelations() {
        // Arrange: an existing order item is moved to another order and menu item.
        UUID orderItemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID menuItemId = UUID.randomUUID();
        OrderItem existing = orderItem(orderItemId, 1, "Old notes", order(orderId, "ORD-0001", OrderType.DINE_IN, OrderStatus.PENDING, PaymentStatus.PENDING, null, null, null, null, null, null), menuItem(menuItemId, "Pizza", true));
        Order order = order(orderId, "ORD-0001", OrderType.DINE_IN, OrderStatus.PENDING, PaymentStatus.PENDING, null, null, null, null, null, null);
        MenuItem menuItem = menuItem(menuItemId, "Pizza", true);
        OrderItemRequestDto dto = new OrderItemRequestDto(3, "New notes", orderId, menuItemId);
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(existing));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        when(orderItemRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: update the order item.
        OrderItemResponseDto result = service.updateOrderItem(orderItemId, dto);

        // Assert: the item is updated and still points to the intended relations.
        assertThat(result.getQuantity()).isEqualTo(3);
        assertThat(result.getMenuItemName()).isEqualTo("Pizza");
    }

    @Test
    void deleteOrderItem_success_deletesItem() {
        // Arrange: an existing order item is returned by the repository.
        UUID orderItemId = UUID.randomUUID();
        OrderItem existing = orderItem(orderItemId, 1, "Notes", order(UUID.randomUUID(), "ORD-0001", OrderType.DINE_IN, OrderStatus.PENDING, PaymentStatus.PENDING, null, null, null, null, null, null), menuItem(UUID.randomUUID(), "Pizza", true));
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(existing));

        // Act: delete the order item.
        service.deleteOrderItem(orderItemId);

        // Assert: the repository delete method is called.
        verify(orderItemRepository).delete(existing);
    }

    @Test
    void createOrderWithItems_success_createsOrderAndItems() {
        // Arrange: one order request and two item requests.
        UUID tableId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID menuItemId1 = UUID.randomUUID();
        UUID menuItemId2 = UUID.randomUUID();
        RestaurantTable table = table(tableId, "Table 1", "TAB-0001", true);
        User creator = user(creatorId, "john.doe", "john.doe@test.local");
        MenuItem menuItem1 = menuItem(menuItemId1, "Pizza", true);
        MenuItem menuItem2 = menuItem(menuItemId2, "Pasta", true);
        OrderRequestDto orderDto = new OrderRequestDto(
                OrderType.DINE_IN,
                OrderStatus.CONFIRMED,
                PaymentStatus.PAID,
                "Notes",
                null,
                tableId,
                creatorId,
                null,
                null
        );
        List<OrderItemRequestDto> itemDtos = List.of(
                new OrderItemRequestDto(1, "First item", null, menuItemId1),
                new OrderItemRequestDto(2, "Second item", null, menuItemId2)
        );
        AtomicReference<Order> savedOrderRef = new AtomicReference<>();

        when(restaurantTableRepository.findById(tableId)).thenReturn(Optional.of(table));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(orderId);
            saved.setOrderItems(new ArrayList<>());
            savedOrderRef.set(saved);
            return saved;
        });
        when(menuItemRepository.findById(menuItemId1)).thenReturn(Optional.of(menuItem1));
        when(menuItemRepository.findById(menuItemId2)).thenReturn(Optional.of(menuItem2));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            savedOrderRef.get().getOrderItems().add(saved);
            return saved;
        });
        when(orderRepository.findById(orderId)).thenAnswer(invocation -> Optional.of(savedOrderRef.get()));

        // Act: create the order and attach its items.
        OrderResponseDto result = service.createOrderWithItems(orderDto, itemDtos);

        // Assert: the returned order contains both created items.
        assertThat(result.getId()).isEqualTo(orderId);
        assertThat(result.getOrderItems()).hasSize(2);
        assertThat(result.getOrderItems()).extracting(OrderItemResponseDto::getMenuItemName)
                .containsExactly("Pizza", "Pasta");
    }

    private static Order orderWithItems() {
        // Helper: build an order with one nested item for mapping tests.
        UUID orderId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID updaterId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID menuItemId = UUID.randomUUID();

        CategoryMenu category = new CategoryMenu();
        category.setId(categoryId);
        category.setCategoryName("Mains");
        category.setSortOrder(1);
        category.setActive(true);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(menuItemId);
        menuItem.setName("Pizza");
        menuItem.setActive(true);
        menuItem.setCategory(category);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setQuantity(2);
        orderItem.setNotes("Cheese");
        orderItem.setMenuItem(menuItem);

        Order order = new Order();
        order.setId(orderId);
        order.setPublicCode("ORD-0001");
        order.setTypeOrder(OrderType.DINE_IN);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setNotes("Notes");
        order.setRestaurantTable(table(tableId, "Table 1", "TAB-0001", true));
        order.setCreatedBy(user(creatorId, "creator", "creator@test.local"));
        order.setUpdatedBy(user(updaterId, "updater", "updater@test.local"));
        order.setOrderItems(List.of(orderItem));
        orderItem.setOrder(order);
        return order;
    }

    private static Order order(UUID id,
                               String publicCode,
                               OrderType typeOrder,
                               OrderStatus status,
                               PaymentStatus paymentStatus,
                               String notes,
                               String deliveryAddress,
                               String phone,
                               RestaurantTable table,
                               User createdBy,
                               User updatedBy) {
        // Helper: create a minimal order entity for test setup.
        Order order = new Order();
        order.setId(id);
        order.setPublicCode(publicCode);
        order.setTypeOrder(typeOrder);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setNotes(notes);
        order.setDeliveryAddress(deliveryAddress);
        order.setPhone(phone);
        order.setRestaurantTable(table);
        order.setCreatedBy(createdBy);
        order.setUpdatedBy(updatedBy);
        return order;
    }

    private static RestaurantTable table(UUID id, String label, String publicCode, Boolean active) {
        // Helper: create a minimal restaurant table entity for test setup.
        RestaurantTable table = new RestaurantTable();
        table.setId(id);
        table.setLabel(label);
        table.setPublicCode(publicCode);
        table.setActive(active);
        return table;
    }

    private static User user(UUID id, String username, String email) {
        // Helper: create a minimal user entity for test setup.
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRoles(Role.CUSTOMER);
        return user;
    }

    private static MenuItem menuItem(UUID id, String name, boolean active) {
        // Helper: create a minimal menu item with only the fields needed by the service.
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setName(name);
        menuItem.setActive(active);
        return menuItem;
    }

    private static MenuItem menuItem(UUID id, String name, boolean active, CategoryMenu category) {
        // Helper: create a menu item with a category for response mapping.
        MenuItem menuItem = menuItem(id, name, active);
        menuItem.setCategory(category);
        return menuItem;
    }

    private static OrderItem orderItem(UUID id, Integer quantity, String notes, Order order, MenuItem menuItem) {
        // Helper: create a minimal order item for response mapping tests.
        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setQuantity(quantity);
        orderItem.setNotes(notes);
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);
        return orderItem;
    }

    private static void injectNestedMapper(OrderMapper mapper, OrderItemMapper nestedMapper) {
        // Helper: wire the nested mapper used by the generated OrderMapperImpl.
        try {
            Field field = mapper.getClass().getDeclaredField("orderItemMapper");
            field.setAccessible(true);
            field.set(mapper, nestedMapper);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to inject nested mapper into OrderMapperImpl", ex);
        }
    }
}
