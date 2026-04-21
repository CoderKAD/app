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
import java.util.List;

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

        if (users.isEmpty() || tables.isEmpty()) {
            return Collections.emptyList();
        }

        List<Reservation> reservations = new ArrayList<>();
        ReservationStatus[] statuses = ReservationStatus.values();
        LocalDateTime baseStart = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);

        for (int index = 0; index < DEFAULT_RESERVATION_COUNT; index++) {
            Reservation reservation = new Reservation();
            int durationMinutes = faker.options().option(60, 90, 120, 180);
            int people = faker.options().option(2, 4, 4, 6, 8, 10);
            LocalDateTime startAt = baseStart.plusDays(index % 10L).plusHours(11 + (index % 6));
            LocalDateTime endAt = startAt.plusMinutes(durationMinutes);
            ReservationStatus status = statuses[index % statuses.length];

            reservation.setReservationCode(
                    PublicCodeGenerator.generateReservationCode(
                            reservationRepository.count() + index + 1L,
                            reservationRepository::existsByReservationCode));
            reservation.setNumberOfPeople(people);
            reservation.setCustomerName(faker.name().firstName() + " " + faker.name().lastName());
            reservation.setCustomerPhone(String.format("06%08d", faker.number().numberBetween(0, 100000000)));
            reservation.setEmailCustomer(faker.internet().emailAddress());
            reservation.setStartAt(startAt);
            reservation.setEndAt(endAt);
            reservation.setDurationMinutes(durationMinutes);
            reservation.setStatus(status);
            reservation.setNotes(faker.lorem().sentence(10));
            reservation.setBufferTimeMinutes(faker.options().option(15, 20, 30, 45));
            reservation.setCreatedBy(users.get(index % users.size()));
            reservation.setUpdatedBy(users.get((index + 1) % users.size()));
            reservation.setTables(selectTables(tables, people, index));
            applyStatusTimestamps(reservation, status, startAt, endAt);
            reservations.add(reservation);
        }

        return reservationRepository.saveAll(reservations);
    }

    private List<RestaurantTable> selectTables(List<RestaurantTable> tables, int people, int index) {
        List<RestaurantTable> selectedTables = new ArrayList<>();
        RestaurantTable primaryTable = tables.get(index % tables.size());
        selectedTables.add(primaryTable);

        if (people > 6 && tables.size() > 1) {
            RestaurantTable secondaryTable = tables.get((index + 1) % tables.size());
            if (!secondaryTable.getId().equals(primaryTable.getId())) {
                selectedTables.add(secondaryTable);
            }
        }

        return selectedTables;
    }

    private void applyStatusTimestamps(Reservation reservation,
                                       ReservationStatus status,
                                       LocalDateTime startAt,
                                       LocalDateTime endAt) {
        if (status == ReservationStatus.CONFIRMED) {
            reservation.setConfirmedAt(startAt.minusHours(2));
        } else if (status == ReservationStatus.CANCELLED) {
            reservation.setCancelledAt(startAt.minusHours(4));
            reservation.setCancelReason(faker.lorem().sentence(6));
        } else if (status == ReservationStatus.COMPLETED) {
            reservation.setConfirmedAt(startAt.minusHours(3));
        } else if (status == ReservationStatus.NO_SHOW) {
            reservation.setConfirmedAt(endAt.minusHours(1));
        }
    }
}
