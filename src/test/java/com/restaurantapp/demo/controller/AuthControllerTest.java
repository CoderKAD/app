package com.restaurantapp.demo.controller;

import com.restaurantapp.demo.dto.ResponseDto.authRespons.LoginResponse;
import com.restaurantapp.demo.dto.requestDto.auth.LoginRequest;
import com.restaurantapp.demo.dto.requestDto.auth.RegisterRequest;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void register_returnsCreatedAndDefaultsCustomerRole() {
        AuthController controller = new AuthController(userRepository, passwordEncoder);
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "Password1!", null);
        User saved = new User();
        saved.setRoles(Role.CUSTOMER);

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<LoginResponse> result = controller.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRoles()).isEqualTo(Role.CUSTOMER);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(new LoginResponse(null, Role.CUSTOMER));
    }

    @Test
    void register_duplicateUsername_throwsBadRequest() {
        AuthController controller = new AuthController(userRepository, passwordEncoder);
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "Password1!", null);

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> controller.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void login_returnsRoleForValidCredentials() {
        AuthController controller = new AuthController(userRepository, passwordEncoder);
        LoginRequest request = new LoginRequest("alice", "Password1!");
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hashed-password");
        user.setRoles(Role.ADMIN);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "hashed-password")).thenReturn(true);

        ResponseEntity<LoginResponse> result = controller.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(new LoginResponse(null, Role.ADMIN));
    }

    @Test
    void login_invalidCredentials_throwsUnauthorized() {
        AuthController controller = new AuthController(userRepository, passwordEncoder);
        LoginRequest request = new LoginRequest("alice", "wrong");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
