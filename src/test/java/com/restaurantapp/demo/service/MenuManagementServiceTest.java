package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.CategoryMenuResponseDto;
import com.restaurantapp.demo.dto.requestDto.CategoryMenuRequestDto;
import com.restaurantapp.demo.entity.CategoryMenu;
import com.restaurantapp.demo.mapper.CategoryMenuMapper;
import com.restaurantapp.demo.mapper.MenuItemMapper;
import com.restaurantapp.demo.repository.CategoryMenuRepository;
import com.restaurantapp.demo.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuManagementServiceTest {

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
    void createCategory_success() {
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("Beverages", 1, true);

        when(categoryMenuRepository.existsByCategoryNameIgnoreCase("Beverages")).thenReturn(false);
        when(categoryMenuRepository.existsBySortOrder(1)).thenReturn(false);

        CategoryMenu entity = new CategoryMenu(null, "Beverages", 1, true, null, null, null);
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        CategoryMenu saved = new CategoryMenu(id, "Beverages", 1, true, now, now, null);

        when(categoryMenuMapper.toEntity(dto)).thenReturn(entity);
        when(categoryMenuRepository.save(entity)).thenReturn(saved);

        CategoryMenuResponseDto expected = new CategoryMenuResponseDto(
                saved.getId(),
                saved.getCategoryName(),
                saved.getSortOrder(),
                saved.getActive(),
                saved.getCreatedAt(),
                saved.getUpdatedAt());
        when(categoryMenuMapper.toDto(saved)).thenReturn(expected);

        CategoryMenuResponseDto result = menuManagementService.createCategory(dto);

        assertThat(result).isEqualTo(expected);
        verify(categoryMenuRepository).save(entity);
    }

    @Test
    void createCategory_whenNameExists_throws() {
        CategoryMenuRequestDto dto = new CategoryMenuRequestDto("Beverages", 1, true);

        when(categoryMenuRepository.existsByCategoryNameIgnoreCase("Beverages")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> menuManagementService.createCategory(dto));

        verify(categoryMenuRepository, never()).save(any());
    }
}
