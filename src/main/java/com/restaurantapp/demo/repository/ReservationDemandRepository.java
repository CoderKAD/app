package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.ReservationDemand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationDemandRepository extends JpaRepository<ReservationDemand, UUID> {
}
