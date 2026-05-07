package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.UserResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.UserWithRoleInfoResponseDto;
import com.restaurantapp.demo.dto.requestDto.UserRequestDto;
import com.restaurantapp.demo.entity.Staff;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.mapper.StaffMapper;
import com.restaurantapp.demo.mapper.UserMapper;
import com.restaurantapp.demo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       StaffMapper staffMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.staffMapper = staffMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponseDto> getAllUsers() {
        return userMapper.toDto(userRepository.findAll());
    }

    public List<UserWithRoleInfoResponseDto> getAllUsersWithRoleInfo() {
        return userRepository.findAllWithStaff().stream()
                .map(this::toRoleInfoResponse)
                .toList();
    }

    public UserResponseDto createUser(UserRequestDto dto) {
        validateUniqueOnCreate(dto);
        User user = userMapper.toEntity(dto);
        user.setId(null);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRoles(dto.getRoles() == null ? Role.CUSTOMER : dto.getRoles());
        return userMapper.toDto(userRepository.save(user));
    }

    public UserResponseDto updateUser(UUID id, UserRequestDto dto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        validateUniqueOnUpdate(id, dto);
        existing.setUsername(dto.getUsername());
        existing.setEmail(dto.getEmail());
        existing.setRoles(dto.getRoles() == null ? existing.getRoles() : dto.getRoles());
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
        if (dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }
        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + dto.getUsername());
        }
    }

    private void validateUniqueOnUpdate(UUID id, UserRequestDto dto) {
        if (dto.getEmail() != null && userRepository.existsByEmailIgnoreCaseAndIdNot(dto.getEmail(), id)) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }
        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && userRepository.existsByUsernameAndIdNot(dto.getUsername(), id)) {
            throw new IllegalArgumentException("Username already exists: " + dto.getUsername());
        }
    }

    private UserWithRoleInfoResponseDto toRoleInfoResponse(User user) {
        Staff staff = user.getStaff();
        return new UserWithRoleInfoResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                staff == null ? null : staffMapper.toDto(staff)
        );
    }
}
