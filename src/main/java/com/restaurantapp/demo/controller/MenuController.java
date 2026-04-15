package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.CategoryMenuResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.MenuItemResponseDto;
import com.restaurantapp.demo.dto.requestDto.CategoryMenuRequestDto;
import com.restaurantapp.demo.dto.requestDto.MenuItemRequestDto;
import com.restaurantapp.demo.service.MenuManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;
import java.util.UUID;
@Slf4j
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {
    private final MenuManagementService menuManagementService;

    // ==================== Category Endpoints ====================

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryMenuResponseDto>> getAllCategories() {
        return ResponseEntity.ok(menuManagementService.getAllCategories());
    }

    @PostMapping(value = "/categories")
    public ResponseEntity<CategoryMenuResponseDto> createCategory(
            @Valid @RequestBody CategoryMenuRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuManagementService.createCategory(dto));
    }

    @PutMapping(value = "/categories/{id}")
    public ResponseEntity<CategoryMenuResponseDto> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryMenuRequestDto dto
    ) {
        return ResponseEntity.ok(menuManagementService.updateCategory(id, dto));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        menuManagementService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Menu Item Endpoints ====================

    @GetMapping("/items")
    public ResponseEntity<List<MenuItemResponseDto>> getAllMenuItems() {
        return ResponseEntity.ok(menuManagementService.getAllMenuItems());
    }



    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuItemResponseDto> createMenuItem(
            @Valid @ModelAttribute MenuItemRequestDto dto,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) throws IOException {

        return ResponseEntity.ok(menuManagementService.createMenuItem(dto, image));
    }



    @PutMapping(value = "/items/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuItemResponseDto> updateMenuItem(
            @PathVariable UUID id,
            @Valid @ModelAttribute MenuItemRequestDto dto,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) throws IOException {

        return ResponseEntity.ok(menuManagementService.updateMenuItem(id, dto, image));
    }


    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable UUID id) {
        menuManagementService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }
}
