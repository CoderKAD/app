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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuManagementServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private CategoryMenuRepository categoryMenuRepository;

    @Mock
    private MenuItemMapper menuItemMapper;

    @Mock
    private CategoryMenuMapper categoryMenuMapper;

    @InjectMocks
    private MenuManagementService menuManagementService;

    @Test
    void getAllCategories_returnsMappedCategories() {
        CategoryMenu category = new CategoryMenu(UUID.randomUUID(), "Beverages", 1, true, null, null, null);
        CategoryMenuResponseDto response = new CategoryMenuResponseDto(category.getId(), category.getCategoryName(), category.getSortOrder(), category.getActive(), null, null);

        when(categoryMenuRepository.findAll()).thenReturn(List.of(category));
        when(categoryMenuMapper.toDto(List.of(category))).thenReturn(List.of(response));

        List<CategoryMenuResponseDto> result = menuManagementService.getAllCategories();

        assertThat(result).containsExactly(response);
    }

    @Test
    void createCategory_success() {
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("Beverages", 1, true);
        CategoryMenu entity = new CategoryMenu(null, "Beverages", 1, true, null, null, null);
        CategoryMenu saved = new CategoryMenu(UUID.randomUUID(), "Beverages", 1, true, null, null, null);
        CategoryMenuResponseDto expected = new CategoryMenuResponseDto(saved.getId(), saved.getCategoryName(), saved.getSortOrder(), saved.getActive(), null, null);

        when(categoryMenuRepository.existsByCategoryNameIgnoreCase("Beverages")).thenReturn(false);
        when(categoryMenuRepository.existsBySortOrder(1)).thenReturn(false);
        when(categoryMenuMapper.toEntity(dto)).thenReturn(entity);
        when(categoryMenuRepository.save(entity)).thenReturn(saved);
        when(categoryMenuMapper.toDto(saved)).thenReturn(expected);

        CategoryMenuResponseDto result = menuManagementService.createCategory(dto);

        assertThat(result).isSameAs(expected);
        verify(categoryMenuRepository).save(entity);
    }

    @Test
    void updateCategory_success() {
        UUID id = UUID.randomUUID();
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("Desserts", 2, true);
        CategoryMenu existing = new CategoryMenu(id, "Sweets", 1, true, null, null, null);
        CategoryMenu saved = new CategoryMenu(id, "Desserts", 2, true, null, null, null);
        CategoryMenuResponseDto expected = new CategoryMenuResponseDto(id, "Desserts", 2, true, null, null);

        when(categoryMenuRepository.existsByCategoryNameIgnoreCaseAndIdNot("Desserts", id)).thenReturn(false);
        when(categoryMenuRepository.existsBySortOrderAndIdNot(2, id)).thenReturn(false);
        when(categoryMenuRepository.findById(id)).thenReturn(java.util.Optional.of(existing));
        doNothing().when(categoryMenuMapper).updateEntity(dto, existing);
        when(categoryMenuRepository.save(existing)).thenReturn(saved);
        when(categoryMenuMapper.toDto(saved)).thenReturn(expected);

        CategoryMenuResponseDto result = menuManagementService.updateCategory(id, dto);

        assertThat(result).isSameAs(expected);
        verify(categoryMenuRepository).save(existing);
    }

    @Test
    void deleteCategory_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(categoryMenuRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> menuManagementService.deleteCategory(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Category not found");

        verify(categoryMenuRepository, never()).deleteById(any());
    }

    @Test
    void createMenuItem_success_withoutImage() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CategoryMenu category = new CategoryMenu(categoryId, "Main", 1, true, null, null, null);
        MenuItemRequestDto dto = new MenuItemRequestDto("Pizza", "Cheese pizza", 25.0, true, "Hot", categoryId);
        MenuItem entity = new MenuItem(null, "Pizza", "Cheese pizza", 25.0, true, null, "Hot", null, null, null, null);
        MenuItem saved = new MenuItem(UUID.randomUUID(), "Pizza", "Cheese pizza", 25.0, true, null, "Hot", null, null, category, null);
        MenuItemResponseDto expected = new MenuItemResponseDto(saved.getId(), saved.getName(), saved.getDescription(), saved.getPrice(), saved.getActive(), null, saved.getPrepStation(), null, null, categoryId, category.getCategoryName());

        when(categoryMenuRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(menuItemMapper.toEntity(dto)).thenReturn(entity);
        when(menuItemRepository.save(entity)).thenReturn(saved);
        when(menuItemMapper.toDto(saved)).thenReturn(expected);

        MenuItemResponseDto result = menuManagementService.createMenuItem(dto, null);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<MenuItem> captor = ArgumentCaptor.forClass(MenuItem.class);
        verify(menuItemRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isSameAs(category);
        assertThat(captor.getValue().getImageUrl()).isNull();
    }

    @Test
    void updateMenuItem_success_withoutImage() throws Exception {
        UUID categoryId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        CategoryMenu category = new CategoryMenu(categoryId, "Main", 1, true, null, null, null);
        MenuItemRequestDto dto = new MenuItemRequestDto("Burger", "Beef burger", 30.0, true, "Grill", categoryId);
        MenuItem existing = new MenuItem(id, "Old", "Old desc", 20.0, true, "old.png", "Old", null, null, null, null);
        MenuItem saved = new MenuItem(id, "Burger", "Beef burger", 30.0, true, "old.png", "Grill", null, null, category, null);
        MenuItemResponseDto expected = new MenuItemResponseDto(id, "Burger", "Beef burger", 30.0, true, "old.png", "Grill", null, null, categoryId, category.getCategoryName());

        when(menuItemRepository.findById(id)).thenReturn(java.util.Optional.of(existing));
        when(categoryMenuRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        doNothing().when(menuItemMapper).updateEntity(dto, existing);
        when(menuItemRepository.save(existing)).thenReturn(saved);
        when(menuItemMapper.toDto(saved)).thenReturn(expected);

        MenuItemResponseDto result = menuManagementService.updateMenuItem(id, dto, null);

        assertThat(result).isSameAs(expected);
        verify(menuItemRepository).save(existing);
        assertThat(existing.getCategory()).isSameAs(category);
    }

    @Test
    void deleteMenuItem_success_withoutImageCleanup() throws Exception {
        UUID id = UUID.randomUUID();
        MenuItem existing = new MenuItem(id, "Burger", null, 30.0, true, null, null, null, null, null, null);

        when(menuItemRepository.findById(id)).thenReturn(java.util.Optional.of(existing));

        menuManagementService.deleteMenuItem(id);

        verify(menuItemRepository).delete(existing);
    }
}
