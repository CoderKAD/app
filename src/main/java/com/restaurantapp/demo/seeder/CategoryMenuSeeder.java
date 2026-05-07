package com.restaurantapp.demo.seeder;

import com.restaurantapp.demo.entity.CategoryMenu;
import com.restaurantapp.demo.repository.CategoryMenuRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategoryMenuSeeder {

    private static final int DEFAULT_CATEGORY_COUNT = 6;
    private static final List<String> CATEGORY_NAMES = List.of(
            "Starters",
            "Salads",
            "Main Courses",
            "Pasta",
            "Desserts",
            "Drinks"
    );

    private final CategoryMenuRepository categoryMenuRepository;

    public CategoryMenuSeeder(CategoryMenuRepository categoryMenuRepository) {
        this.categoryMenuRepository = categoryMenuRepository;
    }

    public List<CategoryMenu> seed() {
        if (categoryMenuRepository.count() > 0) {
            return categoryMenuRepository.findAll(Sort.by("sortOrder"));
        }

        List<CategoryMenu> categories = new ArrayList<>();
        for (int index = 0; index < DEFAULT_CATEGORY_COUNT; index++) {
            CategoryMenu categoryMenu = new CategoryMenu();
            categoryMenu.setCategoryName(CATEGORY_NAMES.get(index));
            categoryMenu.setSortOrder(index + 1);
            categoryMenu.setActive(Boolean.TRUE);
            categories.add(categoryMenu);
        }

        return categoryMenuRepository.saveAll(categories);
    }
}
