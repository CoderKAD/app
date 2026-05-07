package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.CategoryMenuResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.MenuItemResponseDto;
import com.restaurantapp.demo.dto.requestDto.CategoryMenuRequestDto;
import com.restaurantapp.demo.dto.requestDto.MenuItemRequestDto;
import com.restaurantapp.demo.service.MenuManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    @Mock
    private MenuManagementService menuManagementService;

    @Test
    void getAllCategories_returnsCategories() {
        MenuController controller = new MenuController(menuManagementService);
        CategoryMenuResponseDto response = new CategoryMenuResponseDto();

        when(menuManagementService.getAllCategories()).thenReturn(List.of(response));

        ResponseEntity<List<CategoryMenuResponseDto>> result = controller.getAllCategories();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(menuManagementService).getAllCategories();
    }

    @Test
    void createCategory_returnsCreated() {
        MenuController controller = new MenuController(menuManagementService);
        CategoryMenuRequestDto request = new CategoryMenuRequestDto();
        CategoryMenuResponseDto response = new CategoryMenuResponseDto();

        when(menuManagementService.createCategory(request)).thenReturn(response);

        ResponseEntity<CategoryMenuResponseDto> result = controller.createCategory(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(menuManagementService).createCategory(request);
    }

    @Test
    void updateCategory_returnsOk() {
        MenuController controller = new MenuController(menuManagementService);
        UUID id = UUID.randomUUID();
        CategoryMenuRequestDto request = new CategoryMenuRequestDto();
        CategoryMenuResponseDto response = new CategoryMenuResponseDto();

        when(menuManagementService.updateCategory(eq(id), any())).thenReturn(response);

        ResponseEntity<CategoryMenuResponseDto> result = controller.updateCategory(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(menuManagementService).updateCategory(id, request);
    }

    @Test
    void deleteCategory_returnsNoContent() {
        MenuController controller = new MenuController(menuManagementService);
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.deleteCategory(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(menuManagementService).deleteCategory(id);
    }

    @Test
    void getAllMenuItems_returnsItems() {
        MenuController controller = new MenuController(menuManagementService);
        MenuItemResponseDto response = new MenuItemResponseDto();

        when(menuManagementService.getAllMenuItems()).thenReturn(List.of(response));

        ResponseEntity<List<MenuItemResponseDto>> result = controller.getAllMenuItems();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(menuManagementService).getAllMenuItems();
    }

    @Test
    void createMenuItem_returnsOk() throws IOException {
        MenuController controller = new MenuController(menuManagementService);
        MenuItemRequestDto request = new MenuItemRequestDto();
        MultipartFile image = mock(MultipartFile.class);
        MenuItemResponseDto response = new MenuItemResponseDto();

        when(menuManagementService.createMenuItem(request, image)).thenReturn(response);

        ResponseEntity<MenuItemResponseDto> result = controller.createMenuItem(request, image);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(menuManagementService).createMenuItem(request, image);
    }

    @Test
    void updateMenuItem_returnsOk() throws IOException {
        MenuController controller = new MenuController(menuManagementService);
        UUID id = UUID.randomUUID();
        MenuItemRequestDto request = new MenuItemRequestDto();
        MultipartFile image = mock(MultipartFile.class);
        MenuItemResponseDto response = new MenuItemResponseDto();

        when(menuManagementService.updateMenuItem(eq(id), any(), any())).thenReturn(response);

        ResponseEntity<MenuItemResponseDto> result = controller.updateMenuItem(id, request, image);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(menuManagementService).updateMenuItem(id, request, image);
    }

    @Test
    void deleteMenuItem_returnsNoContent() {
        MenuController controller = new MenuController(menuManagementService);
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.deleteMenuItem(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(menuManagementService).deleteMenuItem(id);
    }
}
