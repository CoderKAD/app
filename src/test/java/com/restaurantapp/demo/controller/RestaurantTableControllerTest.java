package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.RestaurantTableResponseDto;
import com.restaurantapp.demo.dto.requestDto.RestaurantTableRequestDto;
import com.restaurantapp.demo.entity.enums.TableStatus;
import com.restaurantapp.demo.service.RestaurantTableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantTableControllerTest {

    @Mock
    private RestaurantTableService restaurantTableService;

    @Test
    void getAllTables_returnsTables() {
        RestaurantTableController controller = new RestaurantTableController(restaurantTableService);
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(UUID.randomUUID(), "T1", 4, "TAB-0001", true, TableStatus.Available, null, null, null);

        when(restaurantTableService.getAllTables()).thenReturn(List.of(response));

        ResponseEntity<List<RestaurantTableResponseDto>> result = controller.getAllTables();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(restaurantTableService).getAllTables();
    }

    @Test
    void getTableById_returnsTable() {
        RestaurantTableController controller = new RestaurantTableController(restaurantTableService);
        UUID id = UUID.randomUUID();
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(id, "T1", 4, "TAB-0001", true, TableStatus.Available, null, null, null);

        when(restaurantTableService.getTableById(id)).thenReturn(response);

        ResponseEntity<RestaurantTableResponseDto> result = controller.getTableById(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(restaurantTableService).getTableById(id);
    }

    @Test
    void createTable_returnsOk() {
        RestaurantTableController controller = new RestaurantTableController(restaurantTableService);
        RestaurantTableRequestDto request = new RestaurantTableRequestDto();
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(UUID.randomUUID(), "T2", 6, "TAB-0002", true, TableStatus.Reserved, null, null, null);

        when(restaurantTableService.createTable(request)).thenReturn(response);

        ResponseEntity<RestaurantTableResponseDto> result = controller.createTable(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(restaurantTableService).createTable(request);
    }

    @Test
    void updateTable_returnsOk() {
        RestaurantTableController controller = new RestaurantTableController(restaurantTableService);
        UUID id = UUID.randomUUID();
        RestaurantTableRequestDto request = new RestaurantTableRequestDto();
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(id, "T3", 8, "TAB-0003", true, TableStatus.Occupied, null, null, null);

        when(restaurantTableService.updateTable(eq(id), any())).thenReturn(response);

        ResponseEntity<RestaurantTableResponseDto> result = controller.updateTable(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(restaurantTableService).updateTable(id, request);
    }

    @Test
    void deleteTable_returnsNoContent() {
        RestaurantTableController controller = new RestaurantTableController(restaurantTableService);
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.deleteTable(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(restaurantTableService).deleteTable(id);
    }
}
