package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.RestaurantTableResponseDto;
import com.restaurantapp.demo.dto.requestDto.RestaurantTableRequestDto;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.mapper.RestaurantTableMapper;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import com.restaurantapp.demo.util.PublicCodeGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantTableService {

    private final RestaurantTableRepository restaurantTableRepository;
    private final UserRepository userRepository;
    private final RestaurantTableMapper restaurantTableMapper;

    // ==================== Restaurant Table Management ====================

    public List<RestaurantTableResponseDto> getAllTables() {
        return restaurantTableMapper.toDto(restaurantTableRepository.findAll());
    }

    public RestaurantTableResponseDto getTableById(UUID id) {
        return restaurantTableMapper.toDto(findTableById(id));
    }

    public RestaurantTableResponseDto createTable(RestaurantTableRequestDto dto) {
        validateTable(dto);

        RestaurantTable table = restaurantTableMapper.toEntity(dto);
        table.setId(null);
        table.setPublicCode(generatePublicCode());

        // Set user if provided and validate role
        if (dto.getUserId() != null) {
            User user = findUserById(dto.getUserId());
            table.setUser(user);
        }

        return restaurantTableMapper.toDto(restaurantTableRepository.save(table));
    }

    public RestaurantTableResponseDto updateTable(UUID id, RestaurantTableRequestDto dto) {
        validateTable(dto);

        RestaurantTable table = findTableById(id);
        restaurantTableMapper.updateEntity(dto, table);

        // Generate public code if not set
        if (table.getPublicCode() == null || table.getPublicCode().isBlank()) {
            table.setPublicCode(generatePublicCode());
        }

        // Set user if provided and validate role
        if (dto.getUserId() != null) {
            User user = findUserById(dto.getUserId());
            table.setUser(user);
        } else {
            table.setUser(null);
        }

        return restaurantTableMapper.toDto(restaurantTableRepository.save(table));
    }

    public void deleteTable(UUID id) {
        if (!restaurantTableRepository.existsById(id)) {
            throw new EntityNotFoundException("RestaurantTable not found with id: " + id);
        }
        restaurantTableRepository.deleteById(id);
    }

    // ==================== Private Helper Methods ====================

    private RestaurantTable findTableById(UUID id) {
        return restaurantTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RestaurantTable not found with id: " + id));
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    private String generatePublicCode() {
        long nextSequence = restaurantTableRepository.count() + 1;
        return PublicCodeGenerator.generateTableCode(nextSequence, restaurantTableRepository::existsByPublicCode);
    }

    private void validateTable(RestaurantTableRequestDto dto) {
        // Validate seats are reasonable (1-20 seats per table)
        if (dto.getSeats() != null && (dto.getSeats() < 1 || dto.getSeats() > 20)) {
            throw new IllegalArgumentException("Seats must be between 1 and 20.");
        }

    }


}