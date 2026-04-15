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
import com.restaurantapp.demo.entity.enums.OrderType;
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

    // ==================== Order Management ====================

    public List<OrderResponseDto> getAllOrders() {
        // Use repository method that eagerly fetches order items to avoid N+1 problems
        return orderMapper.toDto(orderRepository.findAllBy());
    }

    public OrderResponseDto getOrderById(UUID id) {
        return orderMapper.toDto(findOrderById(id));
    }

    public OrderResponseDto createOrder(OrderRequestDto dto) {
        // Validate required fields depending on order type (dine-in/delivery)
        validateOrderType(dto);

        Order order = orderMapper.toEntity(dto);
        order.setPublicCode(generatePublicCode());

        // Set createdBy / updatedBy when provided
        if (dto.getCreatedById() != null) {
            User creator = findUserById(dto.getCreatedById());
            order.setCreatedBy(creator);
            // if updatedBy not provided, keep same as creator
            order.setUpdatedBy(creator);
        }

        if (dto.getUpdatedById() != null) {
            User updater = findUserById(dto.getUpdatedById());
            order.setUpdatedBy(updater);
        }

        // For delivery orders, validate and normalize phone number
        if (dto.getTypeOrder() == OrderType.DELIVERY) {
            String phoneRaw = dto.getPhone();
            if (phoneRaw == null || phoneRaw.isBlank()) {
                throw new BadRequestException("Phone number is required for delivery orders.");
            }
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            try {
                PhoneNumber phoneProto = phoneUtil.parse(phoneRaw, "MA");
                if (!phoneUtil.isValidNumberForRegion(phoneProto, "MA")) {
                    throw new BadRequestException("Invalid Moroccan phone number.");
                }
                String e164 = phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164);
                order.setPhone(e164);
            } catch (NumberParseException e) {
                throw new BadRequestException("Invalid phone number format.");
            }
        }

        // For dine-in orders, attach the restaurant table (validation already checked existence & activity)
        if (dto.getTypeOrder() == OrderType.DINE_IN && dto.getRestaurantTableId() != null) {
            RestaurantTable table = findRestaurantTableById(dto.getRestaurantTableId());
            order.setRestaurantTable(table);
        }

        return orderMapper.toDto(orderRepository.save(order));
    }

    public OrderResponseDto updateOrder(UUID id, OrderRequestDto dto) {
        // Validate type-specific requirements
        validateOrderType(dto);

        Order order = findOrderById(id);
        orderMapper.updateEntity(dto, order);

        // Update relations if provided
        if (dto.getUpdatedById() != null) {
            User updater = findUserById(dto.getUpdatedById());
            order.setUpdatedBy(updater);
        }

        if (dto.getRestaurantTableId() != null) {
            RestaurantTable table = findRestaurantTableById(dto.getRestaurantTableId());
            order.setRestaurantTable(table);
        }

        return orderMapper.toDto(orderRepository.save(order));
    }

    public void deleteOrder(UUID id) {
        if (!orderRepository.existsById(id)) {
            throw new EntityNotFoundException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }

    // ==================== Order Item Management ====================

    public List<OrderItemResponseDto> getAllOrderItems() {
        return orderItemMapper.toDto(orderItemRepository.findAll());
    }

    public OrderItemResponseDto getOrderItemById(UUID id) {
        return orderItemMapper.toDto(findOrderItemById(id));
    }

    public OrderItemResponseDto createOrderItem(OrderItemRequestDto dto) {
        OrderItem orderItem = orderItemMapper.toEntity(dto);
        orderItem.setId(null);
        orderItem.setOrder(findOrderById(dto.getOrderId()));
        orderItem.setMenuItem(findMenuItemById(dto.getMenuItemId()));
        
        return orderItemMapper.toDto(orderItemRepository.save(orderItem));
    }

    public OrderItemResponseDto updateOrderItem(UUID id, OrderItemRequestDto dto) {
        OrderItem orderItem = findOrderItemById(id);
        orderItemMapper.updateEntity(dto, orderItem);
        orderItem.setOrder(findOrderById(dto.getOrderId()));
        orderItem.setMenuItem(findMenuItemById(dto.getMenuItemId()));
        
        return orderItemMapper.toDto(orderItemRepository.save(orderItem));
    }

    public void deleteOrderItem(UUID id) {
        if (!orderItemRepository.existsById(id)) {
            throw new EntityNotFoundException("OrderItem not found with id: " + id);
        }
        orderItemRepository.deleteById(id);
    }

    // ==================== Private Helper Methods ====================

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

    private void validateOrderType(OrderRequestDto dto) {
        OrderType orderType = dto.getTypeOrder();
        
        if (orderType == null) {
            throw new IllegalArgumentException("Order type is required.");
        }

        if (orderType == OrderType.DINE_IN) {
            validateDineInOrder(dto);
        } else if (orderType == OrderType.DELIVERY) {
            validateDeliveryOrder(dto);
        }
    }

    private void validateDineInOrder(OrderRequestDto dto) {
        if (dto.getRestaurantTableId() == null) {
            throw new IllegalArgumentException("table_id is required for dine-in orders.");
        }
        
        RestaurantTable table = findRestaurantTableById(dto.getRestaurantTableId());
        if (!Boolean.TRUE.equals(table.getActive())) {
            throw new IllegalArgumentException("table_id is not listed in the restaurant table schedule.");
        }
    }

    private void validateDeliveryOrder(OrderRequestDto dto) {
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new IllegalArgumentException("Phone number is required for delivery orders.");
        }
        
        if (dto.getDeliveryAddress() == null || dto.getDeliveryAddress().isBlank()) {
            throw new IllegalArgumentException("Delivery address is required for delivery orders.");
        }

    }
}