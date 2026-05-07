package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByReservationCode(String reservationCode);

    Optional<Reservation> findByReservationCode(String reservationCode);



}
