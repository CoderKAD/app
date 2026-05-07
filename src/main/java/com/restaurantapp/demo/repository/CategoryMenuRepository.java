package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.CategoryMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;



public interface CategoryMenuRepository extends JpaRepository<CategoryMenu, UUID> {
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, UUID id);
    @Query("SELECT MAX (cm.sortOrder) FROM CategoryMenu cm")
    Integer findMaxSortOrder();

}
