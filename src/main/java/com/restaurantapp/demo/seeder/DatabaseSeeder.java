package com.restaurantapp.demo.seeder;

import com.restaurantapp.demo.entity.CategoryMenu;
import com.restaurantapp.demo.entity.MenuItem;
import com.restaurantapp.demo.entity.Order;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

   private final CategoryMenuSeeder categoryMenuSeeder;
    private final MenuItemSeeder menuItemSeeder;
    private final UserSeeder userSeeder;
    private final StaffSeeder staffSeeder;
    private final RestaurantTableSeeder restaurantTableSeeder;
    private final ReservationSeeder reservationSeeder;
    private final ReservationDemandSeeder reservationDemandSeeder;
    private final OrderSeeder orderSeeder;
    private final OrderItemSeeder orderItemSeeder;

     /*public DatabaseSeeder(CategoryMenuSeeder categoryMenuSeeder,
                          MenuItemSeeder menuItemSeeder,
                          UserSeeder userSeeder,
                          StaffSeeder staffSeeder,
                          RestaurantTableSeeder restaurantTableSeeder,
                          ReservationSeeder reservationSeeder,
                          ReservationDemandSeeder reservationDemandSeeder,
                          OrderSeeder orderSeeder,
                          OrderItemSeeder orderItemSeeder) {
        this.categoryMenuSeeder = categoryMenuSeeder;
        this.menuItemSeeder = menuItemSeeder;
        this.userSeeder = userSeeder;
        this.staffSeeder = staffSeeder;
        this.restaurantTableSeeder = restaurantTableSeeder;
        this.reservationSeeder = reservationSeeder;
        this.reservationDemandSeeder = reservationDemandSeeder;
        this.orderSeeder = orderSeeder;
        this.orderItemSeeder = orderItemSeeder;
    }*/

    @Override
    @Transactional
    public void run(String... args) {
        List<User> users = userSeeder.seed();
        staffSeeder.seed(users);
        List<RestaurantTable> tables = restaurantTableSeeder.seed(users);

        List<CategoryMenu> categories = categoryMenuSeeder.seed();
        List<MenuItem> menuItems = menuItemSeeder.seed(categories);

        List<Reservation> reservations = reservationSeeder.seed(users, tables);
        reservationDemandSeeder.seed(reservations, users);

        List<Order> orders = orderSeeder.seed(users, tables);
        orderItemSeeder.seed(orders, menuItems);
    }
}
