package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.StaffResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.UserResponseDto;
import com.restaurantapp.demo.dto.requestDto.StaffRequestDto;
import com.restaurantapp.demo.dto.requestDto.UserRequestDto;
import com.restaurantapp.demo.entity.Staff;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.mapper.StaffMapper;
import com.restaurantapp.demo.mapper.UserMapper;
import com.restaurantapp.demo.repository.StaffRepository;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffCustomerServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffMapper staffMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private StaffCustomerService service;

    @BeforeEach
    void setUp() {
        service = new StaffCustomerService(staffRepository, userRepository, staffMapper, userMapper, passwordEncoder);
    }

    @Test
    void getAllStaff_returnsMappedStaff() {
        Staff staff = staff("CIN-001");
        StaffResponseDto response = new StaffResponseDto(staff.getId(), "John", "Doe", 5000.0, "Manager",
                LocalDate.of(2024, 1, 10), null, "CIN-001", null, null, null);

        when(staffRepository.findAll()).thenReturn(List.of(staff));
        when(staffMapper.toDto(List.of(staff))).thenReturn(List.of(response));

        List<StaffResponseDto> result = service.getAllStaff();

        assertThat(result).containsExactly(response);
        verify(staffRepository).findAll();
    }

    @Test
    void createStaff_persistsWithLinkedUser() {
        UUID userId = UUID.randomUUID();
        StaffRequestDto dto = new StaffRequestDto("John", "Doe", 5000.0, "Manager",
                LocalDate.of(2024, 1, 10), null, "CIN-001", userId);
        User linkedUser = user("john", "john@example.com", Role.ADMIN);
        Staff mapped = staff("CIN-001");
        StaffResponseDto response = new StaffResponseDto(mapped.getId(), "John", "Doe", 5000.0, "Manager",
                LocalDate.of(2024, 1, 10), null, "CIN-001", null, null, userId);

        when(staffRepository.existsByCinIgnoreCase(dto.getCin())).thenReturn(false);
        when(staffMapper.toEntity(dto)).thenReturn(mapped);
        when(userRepository.findById(userId)).thenReturn(Optional.of(linkedUser));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(staffMapper.toDto(any(Staff.class))).thenReturn(response);

        StaffResponseDto result = service.createStaff(dto);

        ArgumentCaptor<Staff> captor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository).save(captor.capture());
        assertThat(captor.getValue().getCin()).isEqualTo("CIN-001");
        assertThat(captor.getValue().getUser()).isSameAs(linkedUser);
        assertThat(result).isSameAs(response);
    }

    @Test
    void updateStaff_updatesExistingStaffAndRebindsUser() {
        UUID staffId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Staff existing = staff("CIN-OLD");
        StaffRequestDto dto = new StaffRequestDto("Jane", "Roe", 6500.0, "Supervisor",
                LocalDate.of(2024, 2, 1), null, "CIN-NEW", userId);
        User linkedUser = user("jane", "jane@example.com", Role.ADMIN);
        StaffResponseDto response = new StaffResponseDto(staffId, "Jane", "Roe", 6500.0, "Supervisor",
                LocalDate.of(2024, 2, 1), null, "CIN-NEW", null, null, userId);

        when(staffRepository.findById(staffId)).thenReturn(Optional.of(existing));
        when(staffRepository.existsByCinIgnoreCaseAndIdNot(dto.getCin(), staffId)).thenReturn(false);
        doAnswer(invocation -> {
            StaffRequestDto request = invocation.getArgument(0);
            Staff entity = invocation.getArgument(1);
            entity.setFirstName(request.getFirstName());
            entity.setLastName(request.getLastName());
            entity.setSalary(request.getSalary());
            entity.setPosition(request.getPosition());
            entity.setDateJoined(request.getDateJoined());
            entity.setDateLeft(request.getDateLeft());
            entity.setCin(request.getCin());
            return entity;
        }).when(staffMapper).updateEntity(dto, existing);
        when(userRepository.findById(userId)).thenReturn(Optional.of(linkedUser));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(staffMapper.toDto(any(Staff.class))).thenReturn(response);

        StaffResponseDto result = service.updateStaff(staffId, dto);

        assertThat(existing.getCin()).isEqualTo("CIN-NEW");
        assertThat(existing.getUser()).isSameAs(linkedUser);
        assertThat(result).isSameAs(response);
        verify(staffRepository).save(existing);
    }

    @Test
    void deleteStaff_deletesExistingStaff() {
        UUID id = UUID.randomUUID();

        when(staffRepository.existsById(id)).thenReturn(true);

        service.deleteStaff(id);

        verify(staffRepository).deleteById(id);
    }

    @Test
    void getAllUsers_returnsMappedUsers() {
        User user = user("alice", "alice@example.com", Role.CUSTOMER);
        UserResponseDto response = new UserResponseDto(user.getId(), "alice", "alice@example.com", Role.CUSTOMER, null, null);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toDto(List.of(user))).thenReturn(List.of(response));

        List<UserResponseDto> result = service.getAllUsers();

        assertThat(result).containsExactly(response);
        verify(userRepository).findAll();
    }

    @Test
    void createUser_hashesPasswordAndDefaultsRole() {
        UserRequestDto dto = new UserRequestDto("alice", "Password1!", "alice@example.com", null);
        User mapped = user("alice", "alice@example.com", null);
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
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRoles()).isEqualTo(Role.CUSTOMER);
        assertThat(result).isSameAs(response);
    }

    @Test
    void updateUser_replacesPasswordWhenProvided() {
        UUID id = UUID.randomUUID();
        User existing = user("old", "old@example.com", Role.ADMIN);
        UserRequestDto dto = new UserRequestDto("new", "NewPass1!", "new@example.com", null);
        UserResponseDto response = new UserResponseDto(id, "new", "new@example.com", Role.ADMIN, null, null);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(dto.getEmail(), id)).thenReturn(false);
        when(userRepository.existsByUsernameAndIdNot(dto.getUsername(), id)).thenReturn(false);
        doAnswer(invocation -> {
            UserRequestDto request = invocation.getArgument(0);
            User entity = invocation.getArgument(1);
            entity.setUsername(request.getUsername());
            entity.setEmail(request.getEmail());
            return entity;
        }).when(userMapper).updateEntity(dto, existing);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenReturn(response);

        UserResponseDto result = service.updateUser(id, dto);

        assertThat(existing.getUsername()).isEqualTo("new");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
        assertThat(result).isSameAs(response);
    }

    @Test
    void deleteUser_deletesExistingUser() {
        UUID id = UUID.randomUUID();

        when(userRepository.existsById(id)).thenReturn(true);

        service.deleteUser(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void createStaff_duplicateCin_throwsIllegalArgumentException() {
        StaffRequestDto dto = new StaffRequestDto("John", "Doe", 5000.0, "Manager",
                LocalDate.of(2024, 1, 10), null, "CIN-001", null);

        when(staffRepository.existsByCinIgnoreCase(dto.getCin())).thenReturn(true);

        assertThatThrownBy(() -> service.createStaff(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CIN already exists: CIN-001");

        verify(staffMapper, never()).toEntity(any());
    }

    private static User user(String username, String email, Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setRoles(role);
        user.setPasswordHash("hashed");
        return user;
    }

    private static Staff staff(String cin) {
        Staff staff = new Staff();
        staff.setId(UUID.randomUUID());
        staff.setFirstName("John");
        staff.setLastName("Doe");
        staff.setSalary(5000.0);
        staff.setPosition("Manager");
        staff.setDateJoined(LocalDate.of(2024, 1, 10));
        staff.setCin(cin);
        return staff;
    }
}
