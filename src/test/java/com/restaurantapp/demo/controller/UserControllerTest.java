package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.UserResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.UserWithRoleInfoResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.StaffResponseDto;
import com.restaurantapp.demo.dto.requestDto.UserRequestDto;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.service.UserService;
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
class UserControllerTest {

    @Mock
    private UserService userService;

    @Test
    void getAllUsers_returnsUsers() {
        UserController controller = new UserController(userService);
        UserResponseDto response = new UserResponseDto(UUID.randomUUID(), "alice", "alice@example.com", Role.CUSTOMER, null, null);

        when(userService.getAllUsers()).thenReturn(List.of(response));

        ResponseEntity<List<UserResponseDto>> result = controller.getAllUsers();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(userService).getAllUsers();
    }

    @Test
    void getAllUsersWithRoleInfo_returnsUsersWithRoleInfo() {
        UserController controller = new UserController(userService);
        StaffResponseDto staffData = new StaffResponseDto(
                UUID.randomUUID(),
                "John",
                "Doe",
                5000.0,
                "Cashier",
                null,
                null,
                "AB123456",
                null,
                null,
                UUID.randomUUID()
        );
        UserWithRoleInfoResponseDto response = new UserWithRoleInfoResponseDto(
                UUID.randomUUID(),
                "alice",
                "alice@example.com",
                Role.CASHIER,
                null,
                null,
                staffData
        );

        when(userService.getAllUsersWithRoleInfo()).thenReturn(List.of(response));

        ResponseEntity<List<UserWithRoleInfoResponseDto>> result = controller.getAllUsersWithRoleInfo();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(response);
        verify(userService).getAllUsersWithRoleInfo();
    }

    @Test
    void createUser_returnsOk() {
        UserController controller = new UserController(userService);
        UserRequestDto request = new UserRequestDto();
        UserResponseDto response = new UserResponseDto(UUID.randomUUID(), "alice", "alice@example.com", Role.CUSTOMER, null, null);

        when(userService.createUser(request)).thenReturn(response);

        ResponseEntity<UserResponseDto> result = controller.createUser(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(userService).createUser(request);
    }

    @Test
    void updateUser_returnsOk() {
        UserController controller = new UserController(userService);
        UUID id = UUID.randomUUID();
        UserRequestDto request = new UserRequestDto();
        UserResponseDto response = new UserResponseDto(id, "alice", "alice@example.com", Role.CUSTOMER, null, null);

        when(userService.updateUser(eq(id), any())).thenReturn(response);

        ResponseEntity<UserResponseDto> result = controller.updateUser(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(userService).updateUser(id, request);
    }

    @Test
    void deleteUser_returnsNoContent() {
        UserController controller = new UserController(userService);
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.deleteUser(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(userService).deleteUser(id);
    }
}
