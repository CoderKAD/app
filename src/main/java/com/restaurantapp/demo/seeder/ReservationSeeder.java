package com.restaurantapp.demo.seeder;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.ReservationStatus;
import com.restaurantapp.demo.repository.ReservationRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

@Component
public class ReservationSeeder {

    private static final int DEFAULT_RESERVATION_COUNT = 8;

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

        List<Reservation> saved = new ArrayList<>();
        ReservationStatus[] statuses = ReservationStatus.values();
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        for (int index = 0; index < DEFAULT_RESERVATION_COUNT; index++) {
            LocalDate startDate = LocalDate.now().plusDays(index + 1L);
            // choose an hour between 10 and 21 so that endAt (start+2h) <= 23
            int hour = 10 + (index % 12); // will produce hours 10..21 repeatedly
            LocalDateTime startAt = startDate.atTime(hour, 0);

            User owner = users.get(index % users.size());

            int partySizeCandidate = faker.number().numberBetween(1, 6);
            ReservationStatus status = statuses[index % statuses.length];

            // try to find an available table for this startAt
            boolean created = false;

            // sort tables to make selection deterministic
            List<RestaurantTable> sortedTables = new ArrayList<>(tables);
            sortedTables.sort(Comparator.comparing(RestaurantTable::getLabel));

            for (RestaurantTable table : sortedTables) {
                if (!Boolean.TRUE.equals(table.getActive())) continue;
                if (table.getSeats() == null || table.getSeats() < partySizeCandidate) continue;

                // overlap check window
                LocalDateTime startWindow = startAt.minusHours(2);
                LocalDateTime endAt = startAt.plusHours(2);
                List<Reservation> overlaps = reservationRepository.findOverlappingReservations(table.getId(), startWindow, endAt);
                if (overlaps == null || overlaps.isEmpty()) {
                    Reservation reservation = new Reservation();
                    reservation.setPartySize(partySizeCandidate);
                    reservation.setStartAt(startAt);
                    reservation.setStatus(status);
                    reservation.setNotes(faker.lorem().sentence(6));
                    reservation.setCustomerName(faker.name().fullName());

                    // generate phone (local) then normalize to E.164 for Morocco
                    String rawPhone = (faker.number().numberBetween(0,1) == 0 ? "06" : "07") + faker.number().digits(8);
                    try {
                        Phonenumber.PhoneNumber pn = phoneUtil.parse(rawPhone, "MA");
                        if (phoneUtil.isValidNumberForRegion(pn, "MA")) {
                            reservation.setCustomerPhone(phoneUtil.format(pn, PhoneNumberUtil.PhoneNumberFormat.E164));
                        } else {
                            reservation.setCustomerPhone(rawPhone);
                        }
                    } catch (NumberParseException e) {
                        reservation.setCustomerPhone(rawPhone);
                    }

                    reservation.setCreatedBy(owner);
                    reservation.setUpdatedBy(users.get((index + 1) % users.size()));
                    reservation.setTables(List.of(table));

                    saved.add(reservationRepository.save(reservation));
                    created = true;
                    break;
                }
            }

            if (!created) {
                // cannot place this reservation, skip or log — we'll skip
            }
        }

        return saved;
    }
}
