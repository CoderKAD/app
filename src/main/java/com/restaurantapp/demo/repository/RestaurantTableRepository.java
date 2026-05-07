package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.RestaurantTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;
import java.util.List;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {
    boolean existsByPublicCode(String publicCode);

}
