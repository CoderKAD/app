package com.restaurantapp.demo.mapper;

import com.restaurantapp.demo.dto.ResponseDto.OrderItemResponseDto;
import com.restaurantapp.demo.dto.requestDto.OrderItemRequestDto;
import com.restaurantapp.demo.entity.MenuItem;
import com.restaurantapp.demo.entity.Order;
import com.restaurantapp.demo.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(target = "order", source = "orderId", qualifiedByName = "mapOrderId")
    @Mapping(target = "menuItem", source = "menuItemId", qualifiedByName = "mapMenuItemId")
    OrderItem toEntity(OrderItemRequestDto dto);

    @Mapping(target = "order", source = "orderId", qualifiedByName = "mapOrderId")
    @Mapping(target = "menuItem", source = "menuItemId", qualifiedByName = "mapMenuItemId")
    void updateEntity(OrderItemRequestDto dto, @MappingTarget OrderItem entity);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "menuItemId", source = "menuItem.id")
    @Mapping(target = "menuItemName", source = "menuItem.name")
    OrderItemResponseDto toDto(OrderItem entity);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "menuItemId", source = "menuItem.id")
    @Mapping(target = "menuItemName", source = "menuItem.name")
    List<OrderItemResponseDto> toDto(List<OrderItem> entity);

    @Named("mapOrderId")
    default Order mapOrderId(UUID orderId) {
        if (orderId == null) {
            return null;
        }
        Order order = new Order();
        order.setId(orderId);
        return order;
    }

    @Named("mapMenuItemId")
    default MenuItem mapMenuItemId(UUID menuItemId) {
        if (menuItemId == null) {
            return null;
        }
        MenuItem menuItem = new MenuItem();
        menuItem.setId(menuItemId);
        return menuItem;
    }
}