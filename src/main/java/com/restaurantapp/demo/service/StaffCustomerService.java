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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffCustomerService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final StaffMapper staffMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;



    public List<StaffResponseDto> getAllStaff() {

        return staffMapper.toDto(staffRepository.findAll());
    }

    public StaffResponseDto createStaff(StaffRequestDto dto) {
        validateStaffCinForCreate(dto.getCin());
        Staff entity = staffMapper.toEntity(dto);
        entity.setUser(getOptionalUserEntity(dto.getUserId()));
        return staffMapper.toDto(staffRepository.save(entity));
    }

    public StaffResponseDto updateStaff(UUID id, StaffRequestDto dto) {
        Staff existing = getStaffEntity(id);
        validateStaffCinForUpdate(dto.getCin(), id);
        staffMapper.updateEntity(dto, existing);
        existing.setUser(getOptionalUserEntity(dto.getUserId()));
        return staffMapper.toDto(staffRepository.save(existing));
    }

    public void deleteStaff(UUID id) {
        if (!staffRepository.existsById(id)) {
            throw new EntityNotFoundException("Staff not found with id: " + id);
        }
        staffRepository.deleteById(id);
    }



    private void validateStaffCinForCreate(String cin ) {
        if (staffRepository.existsByCinIgnoreCase(cin )) {
            throw new IllegalArgumentException("CIN already exists: " + cin);
        }
    }

    private void validateStaffCinForUpdate(String cin, UUID staffId) {
        if (staffRepository.existsByCinIgnoreCaseAndIdNot(cin, staffId)) {
            throw new IllegalArgumentException("CIN already exists: " + cin);
        }
    }

    private User getUserEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    private User getOptionalUserEntity(UUID id) {
        if (id == null) {
            return null;
        }
        return getUserEntity(id);
    }

    private Staff getStaffEntity(UUID id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found with id: " + id));
    }

    // handling User

    public List<UserResponseDto> getAllUsers() {
        return userMapper.toDto(userRepository.findAll());
    }

    public UserResponseDto createUser(UserRequestDto dto) {
        validateUniqueOnCreate(dto);
        User user = userMapper.toEntity(dto);
        user.setId(null);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        if (user.getRoles() == null) {
            user.setRoles(Role.CUSTOMER);
        }
        return userMapper.toDto(userRepository.save(user));
    }

    public UserResponseDto updateUser(UUID id, UserRequestDto dto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        validateUniqueOnUpdate(id, dto);
        userMapper.updateEntity(dto, existing);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        return userMapper.toDto(userRepository.save(existing));
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private void validateUniqueOnCreate(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }
        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + dto.getUsername());
        }
    }

    private void validateUniqueOnUpdate(UUID id, UserRequestDto dto) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(dto.getEmail(), id)) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }
        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && userRepository.existsByUsernameAndIdNot(dto.getUsername(), id)) {
            throw new IllegalArgumentException("Username already exists: " + dto.getUsername());
        }
    }


}
