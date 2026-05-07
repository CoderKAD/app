package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.CategoryMenuResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.MenuItemResponseDto;
import com.restaurantapp.demo.dto.requestDto.CategoryMenuRequestDto;
import com.restaurantapp.demo.dto.requestDto.MenuItemRequestDto;
import com.restaurantapp.demo.entity.CategoryMenu;
import com.restaurantapp.demo.entity.MenuItem;
import com.restaurantapp.demo.exception.BadRequestException;
import com.restaurantapp.demo.exception.ConflictException;
import com.restaurantapp.demo.mapper.CategoryMenuMapper;
import com.restaurantapp.demo.mapper.MenuItemMapper;
import com.restaurantapp.demo.repository.CategoryMenuRepository;
import com.restaurantapp.demo.repository.MenuItemRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuManagementService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryMenuRepository categoryMenuRepository;
    private final MenuItemMapper menuItemMapper;
    private final CategoryMenuMapper categoryMenuMapper;
    private final ImageService imageService;

    // ================= CATEGORY =================

    public List<CategoryMenuResponseDto> getAllCategories() {
        return categoryMenuMapper.toDto(
                categoryMenuRepository.findAll(Sort.by("sortOrder"))
        );
    }

    public CategoryMenuResponseDto createCategory(CategoryMenuRequestDto dto) {

        validateCategoryName(dto.getCategoryName());
        checkCategoryDuplicate(dto.getCategoryName(), null);

        CategoryMenu category = categoryMenuMapper.toEntity(dto);
        category.setSortOrder(nextSortOrder());

        return categoryMenuMapper.toDto(categoryMenuRepository.save(category));
    }

    public CategoryMenuResponseDto updateCategory(UUID id, CategoryMenuRequestDto dto) {
        CategoryMenu existing = findCategoryById(id);
        validateCategoryName(dto.getCategoryName());

        checkCategoryDuplicate(dto.getCategoryName(), id);
        categoryMenuMapper.updateEntity(dto, existing);

        return categoryMenuMapper.toDto(categoryMenuRepository.save(existing));
    }

    public void deleteCategory(UUID id) {
        CategoryMenu category = findCategoryById(id);
        categoryMenuRepository.delete(category);
    }

    // ================= MENU ITEMS =================

    public List<MenuItemResponseDto> getAllMenuItems() {
        return menuItemMapper.toDto(menuItemRepository.findAll());
    }

    public MenuItemResponseDto createMenuItem(MenuItemRequestDto dto, MultipartFile image) throws IOException {
        CategoryMenu category = findCategoryById(dto.getCategoryId());

        MenuItem item = menuItemMapper.toEntity(dto);
        item.setCategory(category);
        item.setImageUrl(imageService.uploadImageIfPresent(image));
        return menuItemMapper.toDto(menuItemRepository.save(item));
    }

    public MenuItemResponseDto updateMenuItem(UUID id, MenuItemRequestDto dto, MultipartFile image) throws IOException {
        MenuItem existing = findMenuItemById(id);
        CategoryMenu category = findCategoryById(dto.getCategoryId());

        menuItemMapper.updateEntity(dto, existing);
        existing.setCategory(category);

        imageService.updateImageIfNeeded(existing, image);

        return menuItemMapper.toDto(menuItemRepository.save(existing));
    }

    public void deleteMenuItem(UUID id) {
        MenuItem existing = findMenuItemById(id);

        imageService.deleteImageIfExists(existing.getImageUrl());
        menuItemRepository.delete(existing);
    }

    // ================= HELPER METHODS =================

    private void validateCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BadRequestException("Category name cannot be empty");
        }
    }

    private void checkCategoryDuplicate(String name, UUID id) {
        boolean exists = (id == null)
                ? categoryMenuRepository.existsByCategoryNameIgnoreCase(name)
                : categoryMenuRepository.existsByCategoryNameIgnoreCaseAndIdNot(name, id);

        if (exists) {
            throw new ConflictException("Category already exists: " + name);
        }
    }



    private CategoryMenu findCategoryById(UUID id) {
        return categoryMenuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

    private MenuItem findMenuItemById(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found with id: " + id));
    }

    private Integer nextSortOrder() {
        return Optional.ofNullable(categoryMenuRepository.findMaxSortOrder())
                .orElse(0) + 1;
    }
}
