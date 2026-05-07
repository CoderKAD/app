package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.UserResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.StaffResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.UserWithRoleInfoResponseDto;
import com.restaurantapp.demo.dto.requestDto.UserRequestDto;
import com.restaurantapp.demo.entity.Staff;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.mapper.StaffMapper;
import com.restaurantapp.demo.mapper.UserMapper;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private StaffMapper staffMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, userMapper, staffMapper, passwordEncoder);
    }

    @Test
    void getAllUsers_returnsMappedUsers() {
        User user = user("alice", "alice@example.com", "hashed", Role.ADMIN);
        UserResponseDto response = new UserResponseDto(UUID.randomUUID(), "alice", "alice@example.com", Role.ADMIN, null, null);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toDto(List.of(user))).thenReturn(List.of(response));

        List<UserResponseDto> result = service.getAllUsers();

        assertThat(result).containsExactly(response);
        verify(userRepository).findAll();
        verify(userMapper).toDto(List.of(user));
    }

    @Test
    void getAllUsersWithRoleInfo_returnsUsersAndStaffDetails() {
        User customer = user("customer", "customer@example.com", "hashed", Role.CUSTOMER);

        User staffUser = user("staff", "staff@example.com", "hashed", Role.CASHIER);
        Staff staff = new Staff();
        staff.setId(UUID.randomUUID());
        staff.setFirstName("John");
        staff.setLastName("Doe");
        staff.setCin("AB123456");
        staff.setUser(staffUser);
        staffUser.setStaff(staff);

        StaffResponseDto staffResponseDto = new StaffResponseDto(
                staff.getId(),
                "John",
                "Doe",
                null,
                "Chef",
                null,
                null,
                "AB123456",
                null,
                null,
                staffUser.getId()
        );

        when(userRepository.findAllWithStaff()).thenReturn(List.of(customer, staffUser));
        when(staffMapper.toDto(staff)).thenReturn(staffResponseDto);

        List<UserWithRoleInfoResponseDto> result = service.getAllUsersWithRoleInfo();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo(customer.getUsername());
        assertThat(result.get(0).getRoles()).isEqualTo(Role.CUSTOMER);
        assertThat(result.get(0).getStaffData()).isNull();
        assertThat(result.get(1).getUsername()).isEqualTo(staffUser.getUsername());
        assertThat(result.get(1).getRoles()).isEqualTo(Role.CASHIER);
        assertThat(result.get(1).getStaffData()).isNotNull();
        assertThat(result.get(1).getStaffData().getId()).isEqualTo(staffResponseDto.getId());
        assertThat(result.get(1).getStaffData().getFirstName()).isEqualTo("John");
        assertThat(result.get(1).getStaffData().getLastName()).isEqualTo("Doe");
        assertThat(result.get(1).getStaffData().getPosition()).isEqualTo("Chef");
        assertThat(result.get(1).getStaffData().getCin()).isEqualTo("AB123456");
        assertThat(result.get(1).getStaffData().getUserId()).isEqualTo(staffUser.getId());
        verify(userRepository).findAllWithStaff();
        verify(staffMapper).toDto(staff);
    }

    @Test
    void createUser_hashesPasswordAndDefaultsRole() {
        UserRequestDto dto = new UserRequestDto("alice", "Password1!", "alice@example.com", null);
        User mapped = new User();
        UserResponseDto response = new UserResponseDto(UUID.randomUUID(), "alice", "alice@example.com", Role.CUSTOMER, null, null);

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(dto.getUsername())).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(mapped);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenReturn(response);

        UserResponseDto result = service.createUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRoles()).isEqualTo(Role.CUSTOMER);
        assertThat(result).isSameAs(response);
    }

    @Test
    void createUser_duplicateEmail_throwsIllegalArgumentException() {
        UserRequestDto dto = new UserRequestDto("alice", "Password1!", "alice@example.com", null);

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already exists: alice@example.com");

        verify(userMapper, never()).toEntity(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_updatesExistingUserAndRehashesPassword() {
        UUID id = UUID.randomUUID();
        User existing = user("old-name", "old@example.com", "old-hash", Role.ADMIN);
        UserRequestDto dto = new UserRequestDto("new-name", "NewPass1!", "new@example.com", null);
        UserResponseDto response = new UserResponseDto(id, "new-name", "new@example.com", Role.ADMIN, null, null);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(dto.getEmail(), id)).thenReturn(false);
        when(userRepository.existsByUsernameAndIdNot(dto.getUsername(), id)).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenReturn(response);

        UserResponseDto result = service.updateUser(id, dto);

        assertThat(existing.getUsername()).isEqualTo("new-name");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
        assertThat(existing.getRoles()).isEqualTo(Role.ADMIN);
        assertThat(result).isSameAs(response);
        verify(userRepository).save(existing);
    }

    @Test
    void deleteUser_deletesExistingUser() {
        UUID id = UUID.randomUUID();

        when(userRepository.existsById(id)).thenReturn(true);

        service.deleteUser(id);

        verify(userRepository).deleteById(id);
    }

    private static User user(String username, String email, String passwordHash, Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRoles(role);
        return user;
    }
}
