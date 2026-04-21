package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.UserResponseDto;
import com.restaurantapp.demo.dto.requestDto.UserRequestDto;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.mapper.UserMapper;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsers_returnsMappedUsers() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("john");
        user.setPasswordHash("hash");
        user.setEmail("john@example.com");
        user.setRoles(Role.ADMIN);

        UserResponseDto response = new UserResponseDto(user.getId(), user.getUsername(), user.getEmail(), user.getRoles(), null, null);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toDto(List.of(user))).thenReturn(List.of(response));

        List<UserResponseDto> result = userService.getAllUsers();

        assertThat(result).containsExactly(response);
    }

    @Test
    void createUser_success_usesEncodedPasswordAndDefaultRole() {
        UserRequestDto dto = new UserRequestDto("john", "Passw0rd!", "john@example.com", null);
        User entity = new User();
        User saved = new User();
        saved.setId(UUID.randomUUID());
        saved.setUsername("john");
        saved.setPasswordHash("encoded");
        saved.setEmail("john@example.com");
        saved.setRoles(Role.CUSTOMER);
        UserResponseDto expected = new UserResponseDto(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRoles(), null, null);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(entity);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("encoded");
        when(userRepository.save(entity)).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(expected);

        UserResponseDto result = userService.createUser(dto);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("john");
        assertThat(captor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(captor.getValue().getRoles()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void updateUser_success_updatesPasswordAndKeepsExistingRoleWhenMissing() {
        UUID id = UUID.randomUUID();
        User existing = new User();
        existing.setId(id);
        existing.setUsername("john");
        existing.setPasswordHash("oldHash");
        existing.setEmail("john@example.com");
        existing.setRoles(Role.ADMIN);
        UserRequestDto dto = new UserRequestDto("johnny", "NewPass1!", "johnny@example.com", null);
        User saved = new User();
        saved.setId(id);
        saved.setUsername("johnny");
        saved.setPasswordHash("encoded");
        saved.setEmail("johnny@example.com");
        saved.setRoles(Role.ADMIN);
        UserResponseDto expected = new UserResponseDto(id, saved.getUsername(), saved.getEmail(), saved.getRoles(), null, null);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("johnny@example.com", id)).thenReturn(false);
        when(userRepository.existsByUsernameAndIdNot("johnny", id)).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded");
        when(userRepository.save(existing)).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(expected);

        UserResponseDto result = userService.updateUser(id, dto);

        assertThat(result).isSameAs(expected);
        assertThat(existing.getUsername()).isEqualTo("johnny");
        assertThat(existing.getEmail()).isEqualTo("johnny@example.com");
        assertThat(existing.getPasswordHash()).isEqualTo("encoded");
        assertThat(existing.getRoles()).isEqualTo(Role.ADMIN);
    }

    @Test
    void deleteUser_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).deleteById(any());
    }
}
