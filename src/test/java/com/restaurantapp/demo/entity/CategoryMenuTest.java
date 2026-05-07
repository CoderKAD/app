package com.restaurantapp.demo.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMenuTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void noArgsConstructor_setsActiveToTrue() {
        CategoryMenu category = new CategoryMenu();

        assertThat(category.getActive()).isTrue();
    }

    @Test
    void allArgsConstructor_populatesFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MenuItem item = new MenuItem();
        List<MenuItem> menuItems = List.of(item);

        CategoryMenu category = new CategoryMenu(
                id,
                "Mains",
                1,
                false,
                now,
                now.plusMinutes(1),
                menuItems
        );

        assertThat(category.getId()).isEqualTo(id);
        assertThat(category.getCategoryName()).isEqualTo("Mains");
        assertThat(category.getSortOrder()).isEqualTo(1);
        assertThat(category.getActive()).isFalse();
        assertThat(category.getCreatedAt()).isEqualTo(now);
        assertThat(category.getUpdatedAt()).isEqualTo(now.plusMinutes(1));
        assertThat(category.getMenuItems()).isSameAs(menuItems);
    }

    @Test
    void gettersAndSetters_workForAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        MenuItem item = new MenuItem();

        CategoryMenu category = new CategoryMenu();
        category.setId(id);
        category.setCategoryName("Desserts");
        category.setSortOrder(3);
        category.setActive(false);
        category.setCreatedAt(createdAt);
        category.setUpdatedAt(updatedAt);
        category.setMenuItems(List.of(item));

        assertThat(category.getId()).isEqualTo(id);
        assertThat(category.getCategoryName()).isEqualTo("Desserts");
        assertThat(category.getSortOrder()).isEqualTo(3);
        assertThat(category.getActive()).isFalse();
        assertThat(category.getCreatedAt()).isEqualTo(createdAt);
        assertThat(category.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(category.getMenuItems()).containsExactly(item);
    }

    @Test
    void validCategory_hasNoViolations() {
        CategoryMenu category = new CategoryMenu();
        category.setCategoryName("Starters");
        category.setSortOrder(1);
        category.setActive(true);

        Set<ConstraintViolation<CategoryMenu>> violations = validator.validate(category);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankCategoryName_isRejected() {
        CategoryMenu category = validCategory();
        category.setCategoryName(" ");

        Set<ConstraintViolation<CategoryMenu>> violations = validator.validate(category);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("categoryName");
    }

    @Test
    void nullCategoryName_isRejected() {
        CategoryMenu category = validCategory();
        category.setCategoryName(null);

        Set<ConstraintViolation<CategoryMenu>> violations = validator.validate(category);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("categoryName");
    }

    @Test
    void nullSortOrder_isRejected() {
        CategoryMenu category = validCategory();
        category.setSortOrder(null);

        Set<ConstraintViolation<CategoryMenu>> violations = validator.validate(category);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("sortOrder");
    }

    @Test
    void nonPositiveSortOrder_isRejected() {
        CategoryMenu category = validCategory();
        category.setSortOrder(0);

        Set<ConstraintViolation<CategoryMenu>> violations = validator.validate(category);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("sortOrder");
    }

    @Test
    void nullActive_isRejected() {
        CategoryMenu category = validCategory();
        category.setActive(null);

        Set<ConstraintViolation<CategoryMenu>> violations = validator.validate(category);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("active");
    }

    private static CategoryMenu validCategory() {
        CategoryMenu category = new CategoryMenu();
        category.setCategoryName("Mains");
        category.setSortOrder(1);
        category.setActive(true);
        return category;
    }
}
