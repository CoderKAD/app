package com.restaurantapp.demo.seeder;

import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import com.restaurantapp.demo.repository.ReservationRepository;
import com.restaurantapp.demo.util.PublicCodeGenerator;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class ReservationSeeder {

    private static final int DEFAULT_RESERVATION_COUNT = 16;

    private final ReservationRepository reservationRepository;
    private final Faker faker;

    public ReservationSeeder(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
        this.faker = new Faker();
    }

    public List<Reservation> seed(List<User> users, List<RestaurantTable> tables) {
        if (reservationRepository.count() > 0) {
            return reservationRepository.findAll();
        }

        if (users == null || users.isEmpty() || tables == null || tables.isEmpty()) {
            return Collections.emptyList();
        }

        List<Reservation> reservations = new ArrayList<>();
        ReservationStatus[] statuses = ReservationStatus.values();
        LocalDateTime futureBaseDate = LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();
        List<RestaurantTable> seatingTables = tables.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RestaurantTable::getSeats, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        int totalSeats = seatingTables.stream()
                .mapToInt(this::safeSeats)
                .sum();
        if (totalSeats < 1) {
            return Collections.emptyList();
        }

        long nextReservationSequence = reservationRepository.count() + 1L;

        for (int index = 0; index < DEFAULT_RESERVATION_COUNT; index++) {
            Reservation reservation = new Reservation();
            int durationMinutes = faker.options().option(60, 90, 120, 180);
            int people = Math.min(faker.options().option(2, 4, 4, 6, 8, 10), totalSeats);
            ReservationStatus status = statuses[index % statuses.length];
            int dayOffset = index / 2;
            int slot = index % 2;
            LocalDateTime startAt = futureBaseDate
                    .plusDays(dayOffset)
                    .withHour(slot == 0 ? 12 : 17)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            LocalDateTime endAt = startAt.plusMinutes(durationMinutes).plusMinutes(60);

            reservation.setReservationCode(
                    PublicCodeGenerator.generateReservationCode(
                            nextReservationSequence + index,
                            reservationRepository::existsByReservationCode));
            reservation.setNumberOfPeople(people);
            reservation.setCustomerName(faker.name().firstName() + " " + faker.name().lastName());
            reservation.setCustomerPhone(String.format("06%08d", index + 1));
            reservation.setEmailCustomer("reservation-" + (index + 1) + "@restaurant.local");
            reservation.setStartAt(startAt);
            reservation.setEndAt(endAt);
            reservation.setDurationReservationMinutes(durationMinutes);
            reservation.setStatus(status);
            reservation.setNotes(faker.lorem().sentence(10));
            reservation.setCreatedBy(users.get(index % users.size()));
            reservation.setUpdatedBy(users.get((index + 1) % users.size()));
            reservation.setTables(selectTablesForParty(seatingTables, people));
            applyStatusTimestamps(reservation, status, startAt);
            reservations.add(reservation);
        }

        return reservationRepository.saveAll(reservations);
    }

    private List<RestaurantTable> selectTablesForParty(List<RestaurantTable> tables, int people) {
        List<RestaurantTable> selectedTables = new ArrayList<>();
        int seated = 0;

        for (RestaurantTable table : tables) {
            if (table == null) {
                continue;
            }
            selectedTables.add(table);
            seated += safeSeats(table);
            if (seated >= people) {
                return selectedTables;
            }
        }

        return selectedTables;
    }

    private int safeSeats(RestaurantTable table) {
        return table != null && table.getSeats() != null ? table.getSeats() : 0;
    }

    private void applyStatusTimestamps(Reservation reservation,
                                       ReservationStatus status,
                                       LocalDateTime startAt) {
        if (status == ReservationStatus.CONFIRMED) {
            reservation.setConfirmedAt(startAt.minusHours(2));
        } else if (status == ReservationStatus.CANCELLED) {
            reservation.setCancelledAt(startAt.minusHours(4));
            reservation.setCancelReason(faker.lorem().sentence(6));
        } else if (status == ReservationStatus.NO_SHOW) {
            reservation.setConfirmedAt(startAt.minusHours(1));
        } else if (status == ReservationStatus.COMPLETED) {
            reservation.setConfirmedAt(startAt.minusHours(3));
        } 
    }
}
