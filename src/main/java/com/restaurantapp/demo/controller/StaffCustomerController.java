package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.StaffResponseDto;
import com.restaurantapp.demo.dto.requestDto.StaffRequestDto;
import com.restaurantapp.demo.service.StaffCustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class StaffCustomerController {
    private final StaffCustomerService staffCustomerService;

    public StaffCustomerController(StaffCustomerService staffCustomerService) {
        this.staffCustomerService = staffCustomerService;
    }

    @GetMapping("/staff")
    public ResponseEntity<List<StaffResponseDto>> getAllStaff() {
        return ResponseEntity.ok(staffCustomerService.getAllStaff());
    }

    @PostMapping("/staff")
    public ResponseEntity<StaffResponseDto> createStaff(@Valid @RequestBody StaffRequestDto dto) {
        return ResponseEntity.ok(staffCustomerService.createStaff(dto));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<StaffResponseDto> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody StaffRequestDto dto
    ) {
        return ResponseEntity.ok(staffCustomerService.updateStaff(id, dto));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable UUID id) {
        staffCustomerService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }




}
