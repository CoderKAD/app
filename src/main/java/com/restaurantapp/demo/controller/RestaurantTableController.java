package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.RestaurantTableResponseDto;
import com.restaurantapp.demo.dto.requestDto.RestaurantTableRequestDto;
import com.restaurantapp.demo.service.RestaurantTableService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
public class RestaurantTableController {
    private final RestaurantTableService restaurantTableService;

    public RestaurantTableController(RestaurantTableService restaurantTableService) {
        this.restaurantTableService = restaurantTableService;
    }

    @GetMapping
    public ResponseEntity<List<RestaurantTableResponseDto>> getAllTables() {
        return ResponseEntity.ok(restaurantTableService.getAllTables());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantTableResponseDto> getTableById(@PathVariable UUID id) {
        return ResponseEntity.ok(restaurantTableService.getTableById(id));
    }

    @PostMapping
    public ResponseEntity<RestaurantTableResponseDto> createTable(
            @Valid @RequestBody RestaurantTableRequestDto dto
    ) {
        return ResponseEntity.ok(restaurantTableService.createTable(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantTableResponseDto> updateTable(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantTableRequestDto dto
    ) {
        return ResponseEntity.ok(restaurantTableService.updateTable(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable UUID id) {
        restaurantTableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}
