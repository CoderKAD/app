package com.restaurantapp.demo.seeder;

import com.restaurantapp.demo.entity.CategoryMenu;
import com.restaurantapp.demo.entity.MenuItem;
import com.restaurantapp.demo.repository.MenuItemRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class MenuItemSeeder {

    private static final int ITEMS_PER_CATEGORY = 5;

    private final MenuItemRepository menuItemRepository;
    private final Faker faker;

    public MenuItemSeeder(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
        this.faker = new Faker();
    }

    public List<MenuItem> seed(List<CategoryMenu> categories) {
        List<CategoryMenu> sortedCategories = categories == null
                ? List.of()
                : categories.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CategoryMenu::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(CategoryMenu::getCategoryName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        if (menuItemRepository.count() > 0 || sortedCategories.isEmpty()) {
            return sortMenuItems(menuItemRepository.findAll());
        }

        List<MenuItem> menuItems = new ArrayList<>();
        for (CategoryMenu category : sortedCategories) {
            for (int index = 0; index < ITEMS_PER_CATEGORY; index++) {
                MenuItem menuItem = new MenuItem();
                menuItem.setName(faker.commerce().productName());
                menuItem.setDescription(faker.lorem().sentence(12));
                menuItem.setPrice(Double.parseDouble(faker.commerce().price(25.0, 180.0)));
                menuItem.setActive(Boolean.TRUE);
                menuItem.setImageUrl("https://picsum.photos/seed/" + faker.internet().slug() + "/640/480");
                menuItem.setPrepStation(faker.options().option("Cold Kitchen", "Hot Kitchen", "Grill", "Pastry", "Bar"));
                menuItem.setCategory(category);
                menuItems.add(menuItem);
            }
        }

        return menuItemRepository.saveAll(menuItems);
    }

    private List<MenuItem> sortMenuItems(List<MenuItem> menuItems) {
        return menuItems.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((MenuItem item) -> item.getCategory() == null ? null : item.getCategory().getSortOrder(),
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MenuItem::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}
