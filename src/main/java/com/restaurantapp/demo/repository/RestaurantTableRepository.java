package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {
    boolean existsByPublicCode(String publicCode);
}
