package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    @EntityGraph(attributePaths = {"menuItem"})
    Optional<OrderItem> findById(UUID id);
}