package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByReservationCode(String reservationCode);

    @Query("SELECT r FROM Reservation r JOIN r.tables t WHERE t.id = :tableId AND r.startAt < :endAt AND r.endAt > :startAt")
    List<Reservation> findOverlappingReservations(@Param("tableId") UUID tableId,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt);

    @Query("SELECT DISTINCT r FROM Reservation r JOIN r.tables t WHERE t.id IN :tableIds AND r.startAt < :endAt AND r.endAt > :startAt")
    List<Reservation> findOverlappingReservationsForTables(@Param("tableIds") List<UUID> tableIds,
                                                            @Param("startAt") LocalDateTime startAt,
                                                            @Param("endAt") LocalDateTime endAt);
}
