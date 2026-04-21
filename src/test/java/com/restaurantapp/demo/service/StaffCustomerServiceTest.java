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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.doNothing;
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

    @InjectMocks
    private StaffCustomerService staffCustomerService;

    @Test
    void getAllStaff_returnsMappedStaff() {
        Staff staff = new Staff();
        staff.setId(UUID.randomUUID());
        staff.setFirstName("Jane");
        staff.setLastName("Doe");
        staff.setSalary(5000.0);
        staff.setPosition("Manager");
        staff.setDateJoined(LocalDate.of(2024, 1, 1));
        staff.setCin("CIN1");
        StaffResponseDto response = new StaffResponseDto(staff.getId(), staff.getFirstName(), staff.getLastName(), staff.getSalary(), staff.getPosition(), staff.getDateJoined(), staff.getDateLeft(), staff.getCin(), null, null, null);

        when(staffRepository.findAll()).thenReturn(List.of(staff));
        when(staffMapper.toDto(List.of(staff))).thenReturn(List.of(response));

        List<StaffResponseDto> result = staffCustomerService.getAllStaff();

        assertThat(result).containsExactly(response);
    }

    @Test
    void createStaff_success_setsOptionalUser() {
        StaffRequestDto dto = new StaffRequestDto("Jane", "Doe", 5000.0, "Manager", LocalDate.of(2024, 1, 1), null, "CIN1", null);
        Staff entity = new Staff();
        Staff saved = new Staff();
        saved.setId(UUID.randomUUID());
        saved.setFirstName("Jane");
        saved.setLastName("Doe");
        saved.setSalary(5000.0);
        saved.setPosition("Manager");
        saved.setDateJoined(LocalDate.of(2024, 1, 1));
        saved.setCin("CIN1");
        StaffResponseDto expected = new StaffResponseDto(saved.getId(), saved.getFirstName(), saved.getLastName(), saved.getSalary(), saved.getPosition(), saved.getDateJoined(), saved.getDateLeft(), saved.getCin(), null, null, null);

        when(staffRepository.existsByCinIgnoreCase("CIN1")).thenReturn(false);
        when(staffMapper.toEntity(dto)).thenReturn(entity);
        when(staffRepository.save(entity)).thenReturn(saved);
        when(staffMapper.toDto(saved)).thenReturn(expected);

        StaffResponseDto result = staffCustomerService.createStaff(dto);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Staff> captor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isNull();
    }

    @Test
    void updateStaff_success_linksUser() {
        UUID staffId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Staff existing = new Staff();
        existing.setId(staffId);
        existing.setFirstName("Old");
        existing.setLastName("Name");
        existing.setSalary(4000.0);
        existing.setPosition("Waiter");
        existing.setDateJoined(LocalDate.of(2024, 1, 1));
        existing.setCin("OLD");

        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        user.setPasswordHash("hash");
        user.setEmail("john@example.com");
        user.setRoles(Role.CUSTOMER);
        StaffRequestDto dto = new StaffRequestDto("Jane", "Doe", 5000.0, "Manager", LocalDate.of(2024, 2, 1), null, "CIN2", userId);
        Staff saved = new Staff();
        saved.setId(staffId);
        saved.setFirstName("Jane");
        saved.setLastName("Doe");
        saved.setSalary(5000.0);
        saved.setPosition("Manager");
        saved.setDateJoined(LocalDate.of(2024, 2, 1));
        saved.setCin("CIN2");
        saved.setUser(user);
        StaffResponseDto expected = new StaffResponseDto(staffId, "Jane", "Doe", 5000.0, "Manager", LocalDate.of(2024, 2, 1), null, "CIN2", null, null, userId);

        when(staffRepository.findById(staffId)).thenReturn(Optional.of(existing));
        when(staffRepository.existsByCinIgnoreCaseAndIdNot("CIN2", staffId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(staffMapper.updateEntity(dto, existing)).thenReturn(existing);
        when(staffRepository.save(existing)).thenReturn(saved);
        when(staffMapper.toDto(saved)).thenReturn(expected);

        StaffResponseDto result = staffCustomerService.updateStaff(staffId, dto);

        assertThat(result).isSameAs(expected);
        assertThat(existing.getUser()).isSameAs(user);
    }

    @Test
    void createUser_success_usesDefaultRole() {
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

        UserResponseDto result = staffCustomerService.createUser(dto);

        assertThat(result).isSameAs(expected);
        assertThat(entity.getPasswordHash()).isEqualTo("encoded");
        assertThat(entity.getRoles()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void deleteStaff_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(staffRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> staffCustomerService.deleteStaff(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Staff not found");

        verify(staffRepository, never()).deleteById(any());
    }
}
