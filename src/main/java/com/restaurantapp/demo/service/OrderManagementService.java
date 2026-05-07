package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.OrderItemResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.OrderResponseDto;
import com.restaurantapp.demo.dto.requestDto.OrderItemRequestDto;
import com.restaurantapp.demo.dto.requestDto.OrderRequestDto;
import com.restaurantapp.demo.entity.*;
import com.restaurantapp.demo.entity.enums.OrderStatus;
import com.restaurantapp.demo.entity.enums.OrderType;
import com.restaurantapp.demo.entity.enums.PaymentStatus;
import com.restaurantapp.demo.mapper.OrderItemMapper;
import com.restaurantapp.demo.mapper.OrderMapper;
import com.restaurantapp.demo.repository.MenuItemRepository;
import com.restaurantapp.demo.repository.OrderItemRepository;
import com.restaurantapp.demo.repository.OrderRepository;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import com.restaurantapp.demo.util.PublicCodeGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.restaurantapp.demo.exception.BadRequestException;

@Service
@RequiredArgsConstructor

public class OrderManagementService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    // ==================== Order Management ====================

    public List<OrderResponseDto> getAllOrders() {
        return orderMapper.toDto(orderRepository.findAllBy());
    }

    public OrderResponseDto getOrderById(UUID id) {
        return orderMapper.toDto(findOrderById(id));
    }

    public List<OrderItemResponseDto> getAllOrderItems() {
        return orderItemMapper.toDto(orderItemRepository.findAll());
    }

    public OrderItemResponseDto getOrderItemById(UUID id) {
        return orderItemMapper.toDto(findOrderItemById(id));
    }


    public OrderResponseDto createOrder(OrderRequestDto dto) {
        // 1. Validate order type requirements
        validateOrderType(dto);

        // 2. Normalize phone for delivery orders
        normalizePhoneNumber(dto);

        // 3. Set createdBy (CRITICAL FIX)
        if (dto.getCreatedById() == null) {
            throw new BadRequestException("createdById is required for order creation");
        }
        User creator = findUserById(dto.getCreatedById());

        // 4. Create order
        Order order = orderMapper.toEntity(dto);
        order.setPublicCode(generatePublicCode());
        order.setCreatedBy(creator);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setUpdatedBy(creator);

        return orderMapper.toDto(orderRepository.save(order));
    }


    public OrderResponseDto updateOrder(UUID id, OrderRequestDto dto) {
        Order order = findOrderById(id);
        RestaurantTable currentTable = order.getRestaurantTable();
        User currentCreatedBy = order.getCreatedBy();
        User currentUpdatedBy = order.getUpdatedBy();
        OrderType currentType = order.getTypeOrder();
        OrderStatus currentStatus = order.getStatus();
        PaymentStatus currentPaymentStatus = order.getPaymentStatus();
        String currentPhone = order.getPhone();
        String currentNotes = order.getNotes();
        String currentDeliveryAddress = order.getDeliveryAddress();

        // Validate type-specific requirements only if type changed
        if (dto.getTypeOrder() != null && dto.getTypeOrder() != order.getTypeOrder()) {
            validateOrderType(dto);
        }

        // Normalize phone if provided
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            normalizePhoneNumber(dto);
        }

        // Update relations if provided
        if (dto.getRestaurantTableId() != null) {
            order.setRestaurantTable(findRestaurantTableById(dto.getRestaurantTableId()));
        }

        if (dto.getUpdatedById() != null) {
            order.setUpdatedBy(findUserById(dto.getUpdatedById()));
        }
        orderMapper.updateEntity(dto, order);

        if (dto.getRestaurantTableId() == null) {
            order.setRestaurantTable(currentTable);
        }
        if (dto.getCreatedById() == null) {
            order.setCreatedBy(currentCreatedBy);
        }
        if (dto.getUpdatedById() == null) {
            order.setUpdatedBy(currentUpdatedBy);
        }
        if (dto.getTypeOrder() == null) {
            order.setTypeOrder(currentType);
        }
        if (dto.getStatus() == null) {
            order.setStatus(currentStatus);
        }
        if (dto.getPaymentStatus() == null) {
            order.setPaymentStatus(currentPaymentStatus);
        }
        if (dto.getPhone() == null) {
            order.setPhone(currentPhone);
        }
        if (dto.getNotes() == null) {
            order.setNotes(currentNotes);
        }
        if (dto.getDeliveryAddress() == null) {
            order.setDeliveryAddress(currentDeliveryAddress);
        }

        return orderMapper.toDto(orderRepository.save(order));
    }

    public void deleteOrder(UUID id) {
        Order order = findOrderById(id);
        // Cascade delete will handle order items due to orphanRemoval=true
        orderRepository.delete(order);
    }

    // ==================== Order Item Management ====================

    public OrderItemResponseDto createOrderItem(OrderItemRequestDto dto) {
        Order order = findOrderById(dto.getOrderId());
        MenuItem menuItem = findMenuItemById(dto.getMenuItemId());

        // Validate business rules
        validateOrderItemCreation(order, menuItem);

        OrderItem orderItem = orderItemMapper.toEntity(dto);
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);

        return orderItemMapper.toDto(orderItemRepository.save(orderItem));
    }

    public OrderItemResponseDto updateOrderItem(UUID id, OrderItemRequestDto dto) {
        OrderItem orderItem = findOrderItemById(id);
        Order order = findOrderById(dto.getOrderId());
        MenuItem menuItem = findMenuItemById(dto.getMenuItemId());

        orderItemMapper.updateEntity(dto, orderItem);
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);

        return orderItemMapper.toDto(orderItemRepository.save(orderItem));
    }

    public void deleteOrderItem(UUID id) {
        OrderItem orderItem = findOrderItemById(id);
        orderItemRepository.delete(orderItem);
    }

    // ==================== Bulk Operations ====================


    public OrderResponseDto createOrderWithItems(OrderRequestDto orderDto, List<OrderItemRequestDto> itemDtos) {
        OrderResponseDto orderResponse = createOrder(orderDto);

        // Add order items
        itemDtos.forEach(itemDto -> {
            itemDto.setOrderId(orderResponse.getId());
            createOrderItem(itemDto);
        });

        return getOrderById(orderResponse.getId()); // Refresh with items
    }

    // ==================== Private Helper Methods ====================

    private void validateOrderItemCreation(Order order, MenuItem menuItem) {
        if (!menuItem.getActive()) {
            throw new BadRequestException("Menu item is not available: " + menuItem.getName());
        }
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot add items to completed or cancelled orders");
        }
    }

    private void normalizePhoneNumber(OrderRequestDto dto) {
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            try {
                PhoneNumber phoneProto = phoneUtil.parse(dto.getPhone(), "MA");
                if (!phoneUtil.isValidNumberForRegion(phoneProto, "MA")) {
                    throw new BadRequestException("Invalid Moroccan phone number.");
                }
                String e164 = phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164);
                dto.setPhone(e164);
            } catch (NumberParseException e) {
                throw new BadRequestException("Invalid phone number format: " + e.getMessage());
            }
        }
    }

    private void validateOrderType(OrderRequestDto dto) {


        if (dto.getTypeOrder()== OrderType.DINE_IN) {
            validateDineInOrder(dto);
        } else if (dto.getTypeOrder() == OrderType.DELIVERY) {
            validateDeliveryOrder(dto);
        }
    }

    private void validateDineInOrder(OrderRequestDto dto) {
        if (dto.getRestaurantTableId() == null) {
            throw new BadRequestException("restaurantTableId is required for dine-in orders");
        }
        RestaurantTable table = findRestaurantTableById(dto.getRestaurantTableId());
        if (!Boolean.TRUE.equals(table.getActive())) {
            throw new BadRequestException("Table is not available: " + table.getPublicCode());
        }
    }

    private void validateDeliveryOrder(OrderRequestDto dto) {
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            throw new BadRequestException("Phone number is required for delivery orders");
        }
        if (dto.getDeliveryAddress() == null || dto.getDeliveryAddress().trim().isEmpty()) {
            throw new BadRequestException("Delivery address is required for delivery orders");
        }
    }

    // Existing finder methods remain the same...
    private Order findOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
    }

    private OrderItem findOrderItemById(UUID id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("OrderItem not found with id: " + id));
    }

    private RestaurantTable findRestaurantTableById(UUID id) {
        return restaurantTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RestaurantTable not found with id: " + id));
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    private MenuItem findMenuItemById(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem not found with id: " + id));
    }

    private String generatePublicCode() {
        long nextSequence = orderRepository.count() + 1;
        return PublicCodeGenerator.generateOrderCode(nextSequence, orderRepository::existsByPublicCode);
    }
}
