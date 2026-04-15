package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.CategoryMenuResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.MenuItemResponseDto;
import com.restaurantapp.demo.dto.requestDto.CategoryMenuRequestDto;
import com.restaurantapp.demo.dto.requestDto.MenuItemRequestDto;
import com.restaurantapp.demo.entity.CategoryMenu;
import com.restaurantapp.demo.entity.MenuItem;
import com.restaurantapp.demo.mapper.CategoryMenuMapper;
import com.restaurantapp.demo.mapper.MenuItemMapper;
import com.restaurantapp.demo.repository.CategoryMenuRepository;
import com.restaurantapp.demo.repository.MenuItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.io.FilenameUtils.getExtension;

@Service
@RequiredArgsConstructor
public class MenuManagementService {

    // ================= CONFIG =================
    private static final long MAX_FILE_SIZE_BYTES = 1000L * 1024 * 1024;
    private static final List<String> ALLOWED_MIME_TYPES =
            List.of("image/jpeg", "image/png", "image/jpg");

    private static final String IMAGE_STORAGE_DIR = "uploads/menu-items";

    private final MenuItemRepository menuItemRepository;
    private final CategoryMenuRepository categoryMenuRepository;
    private final MenuItemMapper menuItemMapper;
    private final CategoryMenuMapper categoryMenuMapper;

    private final Tika tika = new Tika();
    private final Path imageStorageRoot = Paths.get(IMAGE_STORAGE_DIR);

    // ================= CATEGORY =================

    public List<CategoryMenuResponseDto> getAllCategories() {
        return categoryMenuMapper.toDto(categoryMenuRepository.findAll());
    }

    public CategoryMenuResponseDto createCategory(CategoryMenuRequestDto dto) {
        validateCategoryUniqueness(dto.getCategoryName(), dto.getSortOrder());

        CategoryMenu entity = categoryMenuMapper.toEntity(dto);
        return categoryMenuMapper.toDto(categoryMenuRepository.save(entity));
    }

    public CategoryMenuResponseDto updateCategory(UUID id, CategoryMenuRequestDto dto) {
        validateCategoryUniquenesForUpdate(id, dto.getCategoryName(), dto.getSortOrder());

        CategoryMenu existing = findCategoryById(id);
        categoryMenuMapper.updateEntity(dto, existing);

        return categoryMenuMapper.toDto(categoryMenuRepository.save(existing));
    }

    public void deleteCategory(UUID id) {
        if (!categoryMenuRepository.existsById(id)) {
            throw new EntityNotFoundException("Category not found: " + id);
        }
        categoryMenuRepository.deleteById(id);
    }

    // ================= MENU ITEMS =================

    public List<MenuItemResponseDto> getAllMenuItems() {
        return menuItemMapper.toDto(menuItemRepository.findAll());
    }

    public MenuItemResponseDto createMenuItem(MenuItemRequestDto dto, MultipartFile image) throws IOException {

        CategoryMenu category = findCategoryById(dto.getCategoryId());

        MenuItem entity = menuItemMapper.toEntity(dto);
        entity.setCategory(category);

        String imageUrl = uploadImage(image);
        entity.setImageUrl(imageUrl);

        return menuItemMapper.toDto(menuItemRepository.save(entity));
    }

    public MenuItemResponseDto updateMenuItem(UUID id, MenuItemRequestDto dto, MultipartFile image) throws IOException {

        MenuItem existing = findMenuItemById(id);
        CategoryMenu category = findCategoryById(dto.getCategoryId());

        String oldImage = existing.getImageUrl();

        menuItemMapper.updateEntity(dto, existing);
        existing.setCategory(category);

        if (image != null && !image.isEmpty()) {
            deleteImageIfExists(oldImage);
            existing.setImageUrl(uploadImage(image));
        }

        return menuItemMapper.toDto(menuItemRepository.save(existing));
    }

    public void deleteMenuItem(UUID id) {
        MenuItem existing = findMenuItemById(id);

        deleteImageIfExists(existing.getImageUrl());
        menuItemRepository.delete(existing);
    }

    // ================= IMAGE HANDLING (FIXED) =================

    private String uploadImage(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return null;
        }

        validateImage(file);
        ensureImageDirectoryExists();

        String mimeType = tika.detect(file.getInputStream());

        String extension = switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/jpg" -> ".jpg";
            default -> "";
        };

        String fileName = UUID.randomUUID() + extension;

        Path targetPath = imageStorageRoot.resolve(fileName).normalize();

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/menu-items/" + fileName;
    }

    private void deleteImageIfExists(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path filePath = imageStorageRoot.resolve(fileName).normalize();

            Files.deleteIfExists(filePath);

        } catch (Exception e) {
            System.err.println("Delete image error: " + e.getMessage());
        }
    }

    private void ensureImageDirectoryExists() throws IOException {
        if (!Files.exists(imageStorageRoot)) {
            Files.createDirectories(imageStorageRoot);
        }
    }

    private void validateImage(MultipartFile file) throws IOException {

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File too large (max 1000MB)");
        }

        String mimeType = tika.detect(file.getInputStream());

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Invalid image type: " + mimeType);
        }
    }

    // ================= CATEGORY VALIDATION =================

    private void validateCategoryUniqueness(String name, Integer sortOrder) {
        if (categoryMenuRepository.existsByCategoryNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Category already exists: " + name);
        }

        if (categoryMenuRepository.existsBySortOrder(sortOrder)) {
            throw new IllegalArgumentException("Sort order already exists: " + sortOrder);
        }
    }

    private void validateCategoryUniquenesForUpdate(UUID id, String name, Integer sortOrder) {
        if (categoryMenuRepository.existsByCategoryNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Category already exists: " + name);
        }

        if (categoryMenuRepository.existsBySortOrderAndIdNot(sortOrder, id)) {
            throw new IllegalArgumentException("Sort order already exists: " + sortOrder);
        }
    }

    // ================= FINDERS =================

    private CategoryMenu findCategoryById(UUID id) {
        return categoryMenuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
    }

    private MenuItem findMenuItemById(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found: " + id));
    }
}