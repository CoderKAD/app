package com.restaurantapp.demo.seeder;

import com.restaurantapp.demo.entity.MenuItem;
import com.restaurantapp.demo.entity.Order;
import com.restaurantapp.demo.entity.OrderItem;
import com.restaurantapp.demo.repository.OrderItemRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class OrderItemSeeder {

    private final OrderItemRepository orderItemRepository;
    private final Faker faker;

    public OrderItemSeeder(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
        this.faker = new Faker();
    }

    public List<OrderItem> seed(List<Order> orders, List<MenuItem> menuItems) {
        if (orderItemRepository.count() > 0) {
            return orderItemRepository.findAll().stream()
                    .sorted(Comparator.comparing((OrderItem item) -> item.getOrder() == null ? null : item.getOrder().getPublicCode(),
                            Comparator.nullsLast(String::compareTo))
                            .thenComparing(item -> item.getMenuItem() == null || item.getMenuItem().getCategory() == null
                                    ? null
                                    : item.getMenuItem().getCategory().getSortOrder(),
                                    Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        }

        List<Order> sortedOrders = orders == null ? Collections.emptyList() : orders.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Order::getPublicCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<MenuItem> sortedMenuItems = menuItems == null ? Collections.emptyList() : menuItems.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((MenuItem item) -> item.getCategory() == null ? null : item.getCategory().getSortOrder(),
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MenuItem::getName, Comparator.nullsLast(String::compareTo)))
                .toList();

        if (sortedOrders.isEmpty() || sortedMenuItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<OrderItem> orderItems = new ArrayList<>();

        for (int index = 0; index < sortedOrders.size(); index++) {
            Order order = sortedOrders.get(index);
            int itemCount = faker.number().numberBetween(1, 4);

            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setMenuItem(sortedMenuItems.get((index + itemIndex) % sortedMenuItems.size()));
                orderItem.setQuantity(faker.number().numberBetween(1, 5));
                orderItem.setNotes(faker.options().option(
                        "No onions",
                        "Extra spicy",
                        "Serve immediately",
                        "Pack separately",
                        "Chef recommendation"
                ));
                orderItems.add(orderItem);
            }
        }

        return orderItemRepository.saveAll(orderItems);
    }
}
