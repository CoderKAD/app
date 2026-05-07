package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.StaffResponseDto;
import com.restaurantapp.demo.dto.requestDto.StaffRequestDto;
import com.restaurantapp.demo.service.StaffCustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffCustomerControllerTest {

    @Mock
    private StaffCustomerService staffCustomerService;

    @Test
    void getAllStaff_returnsStaff() {
        StaffCustomerController controller = new StaffCustomerController(staffCustomerService);
        StaffResponseDto response = new StaffResponseDto(UUID.randomUUID(), "John", "Doe", 5000.0, "Manager",
                LocalDate.of(2024, 1, 10), null, "CIN-001", null, null, null);

        when(staffCustomerService.getAllStaff()).thenReturn(List.of(response));

        ResponseEntity<List<StaffResponseDto>> result = controller.getAllStaff();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(staffCustomerService).getAllStaff();
    }

    @Test
    void createStaff_returnsOk() {
        StaffCustomerController controller = new StaffCustomerController(staffCustomerService);
        StaffRequestDto request = new StaffRequestDto();
        StaffResponseDto response = new StaffResponseDto(UUID.randomUUID(), "John", "Doe", 5000.0, "Manager",
                LocalDate.of(2024, 1, 10), null, "CIN-001", null, null, null);

        when(staffCustomerService.createStaff(request)).thenReturn(response);

        ResponseEntity<StaffResponseDto> result = controller.createStaff(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(staffCustomerService).createStaff(request);
    }

    @Test
    void updateStaff_returnsOk() {
        StaffCustomerController controller = new StaffCustomerController(staffCustomerService);
        UUID id = UUID.randomUUID();
        StaffRequestDto request = new StaffRequestDto();
        StaffResponseDto response = new StaffResponseDto(id, "Jane", "Roe", 6500.0, "Supervisor",
                LocalDate.of(2024, 2, 1), null, "CIN-002", null, null, null);

        when(staffCustomerService.updateStaff(eq(id), any())).thenReturn(response);

        ResponseEntity<StaffResponseDto> result = controller.updateStaff(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(staffCustomerService).updateStaff(id, request);
    }

    @Test
    void deleteStaff_returnsNoContent() {
        StaffCustomerController controller = new StaffCustomerController(staffCustomerService);
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.deleteStaff(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(staffCustomerService).deleteStaff(id);
    }
}
