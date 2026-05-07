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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuManagementServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private CategoryMenuRepository categoryMenuRepository;

    @Mock
    private ImageService imageService;

    private MenuManagementService service;

    private final MenuItemMapper menuItemMapper = Mappers.getMapper(MenuItemMapper.class);
    private final CategoryMenuMapper categoryMenuMapper = Mappers.getMapper(CategoryMenuMapper.class);

    @BeforeEach
    void setUp() {
        // Arrange the service with real mappers and mocked persistence/image dependencies.
        service = new MenuManagementService(
                menuItemRepository,
                categoryMenuRepository,
                menuItemMapper,
                categoryMenuMapper,
                imageService
        );
    }

    @Test
    void getAllCategories_returnsMappedAndSortedCategories() {
        // Arrange: build categories in unsorted order and mock the repository call.
        CategoryMenu desserts = category(UUID.randomUUID(), "Desserts", 2, true);
        CategoryMenu starters = category(UUID.randomUUID(), "Starters", 1, true);
        when(categoryMenuRepository.findAll(Sort.by("sortOrder"))).thenReturn(List.of(starters, desserts));

        // Act: call the service method.
        List<CategoryMenuResponseDto> result = service.getAllCategories();

        // Assert: the service returns DTOs in sort-order order.
        assertThat(result).extracting(CategoryMenuResponseDto::getCategoryName)
                .containsExactly("Starters", "Desserts");
        assertThat(result).extracting(CategoryMenuResponseDto::getSortOrder)
                .containsExactly(1, 2);
    }

    @Test
    void createCategory_success_savesCategoryWithNextSortOrder() {
        // Arrange: a valid category name that does not already exist.
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("Mains", true);
        when(categoryMenuRepository.existsByCategoryNameIgnoreCase("Mains")).thenReturn(false);
        when(categoryMenuRepository.findMaxSortOrder()).thenReturn(5);
        when(categoryMenuRepository.save(any(CategoryMenu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: create the category.
        CategoryMenuResponseDto result = service.createCategory(dto);

        // Assert: the next sort order is used and the category is persisted.
        assertThat(result.getCategoryName()).isEqualTo("Mains");
        assertThat(result.getSortOrder()).isEqualTo(6);
        assertThat(result.getActive()).isTrue();
        verify(categoryMenuRepository).findMaxSortOrder();
        verify(categoryMenuRepository).save(any(CategoryMenu.class));
    }

    @Test
    void createCategory_firstCategoryStartsAtOne() {
        // Arrange: no existing categories, so max sort order is null.
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("Starters", true);
        when(categoryMenuRepository.existsByCategoryNameIgnoreCase("Starters")).thenReturn(false);
        when(categoryMenuRepository.findMaxSortOrder()).thenReturn(null);
        when(categoryMenuRepository.save(any(CategoryMenu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: create the first category.
        CategoryMenuResponseDto result = service.createCategory(dto);

        // Assert: the first category starts at sort order 1.
        assertThat(result.getSortOrder()).isEqualTo(1);
    }

    @Test
    void createCategory_duplicateName_throwsConflictException() {
        // Arrange: the repository reports that the category already exists.
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("Mains", true);
        when(categoryMenuRepository.existsByCategoryNameIgnoreCase("Mains")).thenReturn(true);

        // Act and Assert: duplicate names are rejected with a conflict error.
        assertThatThrownBy(() -> service.createCategory(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Category already exists: Mains");
        verify(categoryMenuRepository, never()).save(any());
    }

    @Test
    void createCategory_nullName_throwsBadRequestException() {
        // Arrange: the request carries a null category name.
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto(null, true);

        // Act and Assert: null names are rejected before duplicate checks run.
        assertThatThrownBy(() -> service.createCategory(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Category name cannot be empty");
        verify(categoryMenuRepository, never()).existsByCategoryNameIgnoreCase(anyString());
        verify(categoryMenuRepository, never()).save(any());
    }

    @Test
    void createCategory_blankName_throwsBadRequestException() {
        // Arrange: the request carries a blank category name.
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("   ", true);

        // Act and Assert: blank names are rejected the same way as null names.
        assertThatThrownBy(() -> service.createCategory(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Category name cannot be empty");
        verify(categoryMenuRepository, never()).save(any());
    }

    @Test
    void updateCategory_success_updatesExistingCategory() {
        // Arrange: an existing category and a new name/state.
        UUID id = UUID.randomUUID();
        CategoryMenu existing = category(id, "Old", 1, true);
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("New", false);
        when(categoryMenuRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryMenuRepository.existsByCategoryNameIgnoreCaseAndIdNot("New", id)).thenReturn(false);
        when(categoryMenuRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: update the category.
        CategoryMenuResponseDto result = service.updateCategory(id, dto);

        // Assert: the entity is updated and saved with the same id.
        assertThat(result.getCategoryName()).isEqualTo("New");
        assertThat(result.getActive()).isFalse();
        assertThat(result.getSortOrder()).isEqualTo(1);
        verify(categoryMenuRepository).existsByCategoryNameIgnoreCaseAndIdNot("New", id);
        verify(categoryMenuRepository).save(existing);
    }

    @Test
    void updateCategory_missingCategory_throwsNotFoundException() {
        // Arrange: repository cannot find the category by id.
        UUID id = UUID.randomUUID();
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("New", true);
        when(categoryMenuRepository.findById(id)).thenReturn(Optional.empty());

        // Act and Assert: missing categories are reported as not found.
        assertThatThrownBy(() -> service.updateCategory(id, dto))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessage("Category not found with id: " + id);
        verify(categoryMenuRepository, never()).save(any());
    }

    @Test
    void updateCategory_duplicateName_throwsConflictException() {
        // Arrange: the id exists, but another category already uses the same name.
        UUID id = UUID.randomUUID();
        CategoryMenu existing = category(id, "Old", 1, true);
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("New", false);
        when(categoryMenuRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryMenuRepository.existsByCategoryNameIgnoreCaseAndIdNot("New", id)).thenReturn(true);

        // Act and Assert: duplicate names on update are rejected.
        assertThatThrownBy(() -> service.updateCategory(id, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Category already exists: New");
        verify(categoryMenuRepository, never()).save(any());
    }

    @Test
    void deleteCategory_success_deletesCategory() {
        // Arrange: the category exists.
        UUID id = UUID.randomUUID();
        CategoryMenu category = category(id, "Desserts", 3, true);
        when(categoryMenuRepository.findById(id)).thenReturn(Optional.of(category));

        // Act: delete the category.
        service.deleteCategory(id);

        // Assert: the repository delete method is called with the loaded entity.
        verify(categoryMenuRepository).delete(category);
    }

    @Test
    void deleteCategory_missing_throwsNotFoundException() {
        // Arrange: repository lookup fails.
        UUID id = UUID.randomUUID();
        when(categoryMenuRepository.findById(id)).thenReturn(Optional.empty());

        // Act and Assert: deletion fails if the category does not exist.
        assertThatThrownBy(() -> service.deleteCategory(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessage("Category not found with id: " + id);
        verify(categoryMenuRepository, never()).delete(any());
    }

    @Test
    void getAllMenuItems_returnsMappedItems() {
        // Arrange: build menu items with a shared category.
        CategoryMenu category = category(UUID.randomUUID(), "Mains", 1, true);
        MenuItem pasta = menuItem(UUID.randomUUID(), "Pasta", "Creamy", 18.5, true, "hot", "/uploads/menu-items/pasta.png", category);
        MenuItem soup = menuItem(UUID.randomUUID(), "Soup", "Warm", 8.0, true, "prep", "/uploads/menu-items/soup.png", category);
        when(menuItemRepository.findAll()).thenReturn(List.of(pasta, soup));

        // Act: fetch all menu items.
        List<MenuItemResponseDto> result = service.getAllMenuItems();

        // Assert: mapper output contains the expected DTO fields.
        assertThat(result).extracting(MenuItemResponseDto::getName).containsExactly("Pasta", "Soup");
        assertThat(result).extracting(MenuItemResponseDto::getCategoryName).containsExactly("Mains", "Mains");
    }

    @Test
    void createMenuItem_success_withImage() throws Exception {
        // Arrange: a known category and a valid image.
        UUID categoryId = UUID.randomUUID();
        CategoryMenu category = category(categoryId, "Mains", 1, true);
        MenuItemRequestDto dto = new MenuItemRequestDto("Pizza", "Cheesy", 12.5, true, "hot", categoryId);
        MockMultipartFile image = new MockMultipartFile("image", "pizza.png", "image/png", new byte[] {1, 2, 3});
        when(categoryMenuRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(imageService.uploadImageIfPresent(image)).thenReturn("/uploads/menu-items/pizza.png");
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: create the menu item.
        MenuItemResponseDto result = service.createMenuItem(dto, image);

        // Assert: the category and image are attached to the saved item.
        assertThat(result.getName()).isEqualTo("Pizza");
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        assertThat(result.getCategoryName()).isEqualTo("Mains");
        assertThat(result.getImageUrl()).isEqualTo("/uploads/menu-items/pizza.png");
        verify(imageService).uploadImageIfPresent(image);
    }

    @Test
    void createMenuItem_withoutImage_keepsImageNull() throws Exception {
        // Arrange: a valid category and no uploaded file.
        UUID categoryId = UUID.randomUUID();
        CategoryMenu category = category(categoryId, "Mains", 1, true);
        MenuItemRequestDto dto = new MenuItemRequestDto("Pizza", "Cheesy", 12.5, true, "hot", categoryId);
        when(categoryMenuRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: create the menu item without an image.
        MenuItemResponseDto result = service.createMenuItem(dto, null);

        // Assert: no image URL is set.
        assertThat(result.getImageUrl()).isNull();
        verify(imageService).uploadImageIfPresent(null);
    }

    @Test
    void createMenuItem_missingCategory_throwsNotFoundException() {
        // Arrange: the category lookup will fail.
        UUID categoryId = UUID.randomUUID();
        MenuItemRequestDto dto = new MenuItemRequestDto("Pizza", "Cheesy", 12.5, true, "hot", categoryId);
        MockMultipartFile image = new MockMultipartFile("image", "pizza.png", "image/png", new byte[] {1, 2, 3});
        when(categoryMenuRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act and Assert: the service stops before image upload or save.
        assertThatThrownBy(() -> service.createMenuItem(dto, image))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessage("Category not found with id: " + categoryId);
        verifyNoInteractions(imageService);
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void updateMenuItem_success_replacesExistingImage() throws Exception {
        // Arrange: existing item, valid category, and a new image.
        UUID menuItemId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryMenu category = category(categoryId, "Updated Category", 2, true);
        MenuItem existing = menuItem(menuItemId, "Old Pizza", "Old desc", 10.0, true, "old", "/uploads/menu-items/old-image.png", category);
        MenuItemRequestDto dto = new MenuItemRequestDto("Updated Pizza", "New desc", 14.0, false, "cold", categoryId);
        MockMultipartFile newImage = new MockMultipartFile("image", "new-pizza.png", "image/png", new byte[] {1, 2, 3});
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(existing));
        when(categoryMenuRepository.findById(categoryId)).thenReturn(Optional.of(category));
        doAnswer(invocation -> {
            MenuItem entity = invocation.getArgument(0);
            entity.setImageUrl("/uploads/menu-items/new-pizza.png");
            return null;
        }).when(imageService).updateImageIfNeeded(existing, newImage);
        when(menuItemRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: update the menu item.
        MenuItemResponseDto result = service.updateMenuItem(menuItemId, dto, newImage);

        // Assert: the new image replaces the old one and the category is preserved.
        assertThat(result.getName()).isEqualTo("Updated Pizza");
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        assertThat(result.getImageUrl()).isEqualTo("/uploads/menu-items/new-pizza.png");
        verify(imageService).updateImageIfNeeded(existing, newImage);
    }

    @Test
    void updateMenuItem_withoutNewImage_keepsExistingImage() throws Exception {
        // Arrange: existing item and no replacement image.
        UUID menuItemId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryMenu category = category(categoryId, "Updated Category", 2, true);
        MenuItem existing = menuItem(menuItemId, "Old Pizza", "Old desc", 10.0, true, "old", "/uploads/menu-items/old-image.png", category);
        MenuItemRequestDto dto = new MenuItemRequestDto("Updated Pizza", "New desc", 14.0, false, "cold", categoryId);
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(existing));
        when(categoryMenuRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(menuItemRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: update without sending a new image.
        MenuItemResponseDto result = service.updateMenuItem(menuItemId, dto, null);

        // Assert: the old image stays in place.
        assertThat(result.getImageUrl()).isEqualTo("/uploads/menu-items/old-image.png");
        verify(imageService).updateImageIfNeeded(existing, null);
    }

    @Test
    void updateMenuItem_missingMenuItem_throwsNotFoundException() {
        // Arrange: the menu item cannot be found.
        UUID menuItemId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        MenuItemRequestDto dto = new MenuItemRequestDto("Updated Pizza", "New desc", 14.0, false, "cold", categoryId);
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.empty());

        // Act and Assert: the service fails before touching category or image logic.
        assertThatThrownBy(() -> service.updateMenuItem(menuItemId, dto, null))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessage("Menu item not found with id: " + menuItemId);
        verifyNoInteractions(categoryMenuRepository);
        verifyNoInteractions(imageService);
    }

    @Test
    void updateMenuItem_missingCategory_throwsNotFoundException() {
        // Arrange: the menu item exists, but the new category does not.
        UUID menuItemId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryMenu existingCategory = category(categoryId, "Old Category", 1, true);
        MenuItem existing = menuItem(menuItemId, "Old Pizza", "Old desc", 10.0, true, "old", "/uploads/menu-items/old-image.png", existingCategory);
        MenuItemRequestDto dto = new MenuItemRequestDto("Updated Pizza", "New desc", 14.0, false, "cold", UUID.randomUUID());
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(existing));
        when(categoryMenuRepository.findById(dto.getCategoryId())).thenReturn(Optional.empty());

        // Act and Assert: missing category stops the update before image processing.
        assertThatThrownBy(() -> service.updateMenuItem(menuItemId, dto, null))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessage("Category not found with id: " + dto.getCategoryId());
        verifyNoInteractions(imageService);
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void deleteMenuItem_success_deletesImageAndItem() {
        // Arrange: an existing menu item with an image.
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryMenu category = category(categoryId, "Desserts", 3, true);
        MenuItem existing = menuItem(id, "Cake", "Sweet", 7.5, true, "cold", "/uploads/menu-items/cake.png", category);
        when(menuItemRepository.findById(id)).thenReturn(Optional.of(existing));

        // Act: delete the item.
        service.deleteMenuItem(id);

        // Assert: the image cleanup runs before the repository delete.
        verify(imageService).deleteImageIfExists("/uploads/menu-items/cake.png");
        verify(menuItemRepository).delete(existing);
    }

    @Test
    void deleteMenuItem_missing_throwsNotFoundException() {
        // Arrange: the menu item id does not exist.
        UUID id = UUID.randomUUID();
        when(menuItemRepository.findById(id)).thenReturn(Optional.empty());

        // Act and Assert: deletion fails with a not-found error.
        assertThatThrownBy(() -> service.deleteMenuItem(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessage("Menu item not found with id: " + id);
        verify(menuItemRepository, never()).delete(any());
        verifyNoInteractions(imageService);
    }

    private static CategoryMenu category(UUID id, String name, Integer sortOrder, Boolean active) {
        // Helper: create a minimal category entity for service tests.
        CategoryMenu category = new CategoryMenu();
        category.setId(id);
        category.setCategoryName(name);
        category.setSortOrder(sortOrder);
        category.setActive(active);
        return category;
    }

    private static MenuItem menuItem(UUID id,
                                     String name,
                                     String description,
                                     Double price,
                                     Boolean active,
                                     String prepStation,
                                     String imageUrl,
                                     CategoryMenu category) {
        // Helper: create a minimal menu item entity for service tests.
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setActive(active);
        item.setPrepStation(prepStation);
        item.setImageUrl(imageUrl);
        item.setCategory(category);
        return item;
    }
}
