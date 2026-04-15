package com.restaurantapp.demo.seeder;

import com.restaurantapp.demo.entity.Order;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.OrderStatus;
import com.restaurantapp.demo.entity.enums.OrderType;
import com.restaurantapp.demo.entity.enums.PaymentStatus;
import com.restaurantapp.demo.repository.OrderRepository;
import com.restaurantapp.demo.util.PublicCodeGenerator;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class OrderSeeder {

    private static final int DEFAULT_ORDER_COUNT = 14;

    private final OrderRepository orderRepository;
    private final Faker faker;

    public OrderSeeder(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        this.faker = new Faker();
    }

    public List<Order> seed(List<User> users, List<RestaurantTable> tables) {
        if (orderRepository.count() > 0) {
            return orderRepository.findAll();
        }

        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Order> orders = new ArrayList<>();
        OrderType[] types = OrderType.values();
        OrderStatus[] statuses = OrderStatus.values();
        PaymentStatus[] paymentStatuses = PaymentStatus.values();

        for (int index = 0; index < DEFAULT_ORDER_COUNT; index++) {
            Order order = new Order();
            OrderType type = types[index % types.length];

            order.setPublicCode(PublicCodeGenerator.generateOrderCode(index + 1L, orderRepository::existsByPublicCode));
            order.setTypeOrder(type);
            order.setStatus(statuses[index % statuses.length]);
            order.setPaymentStatus(paymentStatuses[index % paymentStatuses.length]);
            order.setNotes(faker.lorem().sentence(8));
            order.setPhone(faker.phoneNumber().cellPhone());
            order.setCreatedBy(users.get(index % users.size()));
            order.setUpdatedBy(users.get((index + 1) % users.size()));

            if (type == OrderType.DELIVERY) {
                order.setDeliveryAddress(faker.address().fullAddress());
            } else if (!tables.isEmpty()) {
                order.setRestaurantTable(tables.get(index % tables.size()));
            }

            orders.add(order);
        }

        return orderRepository.saveAll(orders);
    }
}