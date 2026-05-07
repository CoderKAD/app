package com.restaurantapp.demo.seeder;

import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.ReservationDemand;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.DemandStatus;
import com.restaurantapp.demo.repository.ReservationDemandRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.List;

@Component
public class ReservationDemandSeeder {

    private final ReservationDemandRepository reservationDemandRepository;

    public ReservationDemandSeeder(ReservationDemandRepository reservationDemandRepository) {
        this.reservationDemandRepository = reservationDemandRepository;
    }

    public List<ReservationDemand> seed(List<Reservation> reservations, List<User> users) {
        if (reservationDemandRepository.count() > 0) {
            return reservationDemandRepository.findAll();
        }

        List<Reservation> sortedReservations = reservations == null
                ? Collections.emptyList()
                : reservations.stream().filter(Objects::nonNull).toList();
        List<User> sortedUsers = users == null
                ? Collections.emptyList()
                : users.stream().filter(Objects::nonNull).toList();

        if (sortedReservations.isEmpty() || sortedUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<ReservationDemand> demands = new ArrayList<>();
        DemandStatus[] statuses = DemandStatus.values();

        for (int index = 0; index < sortedReservations.size(); index++) {
            ReservationDemand demand = new ReservationDemand();
            demand.setReservation(sortedReservations.get(index));
            demand.setUser(sortedUsers.get(index % sortedUsers.size()));
            demand.setStatus(statuses[index % statuses.length]);
            demands.add(demand);
        }

        return reservationDemandRepository.saveAll(demands);
    }
}
