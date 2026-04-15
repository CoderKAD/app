package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("SELECT r FROM Reservation r JOIN r.tables t WHERE t.id = :tableId AND r.startAt > :startWindow AND r.startAt < :endAt")
    List<Reservation> findOverlappingReservations(@Param("tableId") UUID tableId,
                                                  @Param("startWindow") LocalDateTime startWindow,
                                                  @Param("endAt") LocalDateTime endAt);
}
