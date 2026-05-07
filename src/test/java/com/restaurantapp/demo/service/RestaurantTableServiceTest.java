package com.restaurantapp.demo.service;

import com.restaurantapp.demo.dto.ResponseDto.RestaurantTableResponseDto;
import com.restaurantapp.demo.dto.requestDto.RestaurantTableRequestDto;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import com.restaurantapp.demo.entity.enums.Role;
import com.restaurantapp.demo.entity.enums.TableStatus;
import com.restaurantapp.demo.mapper.RestaurantTableMapper;
import com.restaurantapp.demo.repository.RestaurantTableRepository;
import com.restaurantapp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantTableMapper restaurantTableMapper;

    private RestaurantTableService service;

    @BeforeEach
    void setUp() {
        service = new RestaurantTableService(restaurantTableRepository, userRepository, restaurantTableMapper);
    }

    @Test
    void getAllTables_returnsMappedTables() {
        RestaurantTable table = table("T1", 4, true, TableStatus.Available);
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(table.getId(), "T1", 4, "TAB-0001", true, TableStatus.Available, null, null, null);

        when(restaurantTableRepository.findAll()).thenReturn(List.of(table));
        when(restaurantTableMapper.toDto(List.of(table))).thenReturn(List.of(response));

        List<RestaurantTableResponseDto> result = service.getAllTables();

        assertThat(result).containsExactly(response);
        verify(restaurantTableRepository).findAll();
    }

    @Test
    void getTableById_returnsMappedTable() {
        UUID id = UUID.randomUUID();
        RestaurantTable table = table("T1", 4, true, TableStatus.Available);
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(id, "T1", 4, "TAB-0001", true, TableStatus.Available, null, null, null);

        when(restaurantTableRepository.findById(id)).thenReturn(Optional.of(table));
        when(restaurantTableMapper.toDto(table)).thenReturn(response);

        RestaurantTableResponseDto result = service.getTableById(id);

        assertThat(result).isSameAs(response);
    }

    @Test
    void createTable_setsPublicCodeAndUser() {
        UUID userId = UUID.randomUUID();
        RestaurantTableRequestDto dto = new RestaurantTableRequestDto("T2", 6, true, TableStatus.Available, userId);
        RestaurantTable mapped = table("T2", 6, true, TableStatus.Available);
        User user = new User();
        user.setId(userId);
        user.setRoles(Role.ADMIN);
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(mapped.getId(), "T2", 6, "TAB-0002", true, TableStatus.Available, null, null, userId);

        when(restaurantTableRepository.count()).thenReturn(1L);
        when(restaurantTableRepository.existsByPublicCode(any())).thenReturn(false);
        when(restaurantTableMapper.toEntity(dto)).thenReturn(mapped);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restaurantTableMapper.toDto(any(RestaurantTable.class))).thenReturn(response);

        RestaurantTableResponseDto result = service.createTable(dto);

        ArgumentCaptor<RestaurantTable> captor = ArgumentCaptor.forClass(RestaurantTable.class);
        verify(restaurantTableRepository).save(captor.capture());
        assertThat(captor.getValue().getPublicCode()).isNotBlank();
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(result).isSameAs(response);
    }

    @Test
    void updateTable_replacesUserAndPersists() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RestaurantTable existing = table("T1", 4, true, TableStatus.Reserved);
        RestaurantTableRequestDto dto = new RestaurantTableRequestDto("T3", 8, true, TableStatus.Occupied, userId);
        User user = new User();
        user.setId(userId);
        user.setRoles(Role.CASHIER);
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(id, "T3", 8, "TAB-0003", true, TableStatus.Occupied, null, null, userId);

        when(restaurantTableRepository.findById(id)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> {
            RestaurantTableRequestDto request = invocation.getArgument(0);
            RestaurantTable entity = invocation.getArgument(1);
            entity.setLabel(request.getLabel());
            entity.setSeats(request.getSeats());
            entity.setActive(request.getActive());
            entity.setStatus(request.getStatus());
            return entity;
        }).when(restaurantTableMapper).updateEntity(dto, existing);
        when(restaurantTableRepository.count()).thenReturn(2L);
        when(restaurantTableRepository.existsByPublicCode(any())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restaurantTableMapper.toDto(any(RestaurantTable.class))).thenReturn(response);

        RestaurantTableResponseDto result = service.updateTable(id, dto);

        assertThat(existing.getLabel()).isEqualTo("T3");
        assertThat(existing.getUser()).isSameAs(user);
        assertThat(result).isSameAs(response);
        verify(restaurantTableRepository).save(existing);
    }

    @Test
    void updateTable_preservesExistingUser_whenUserIdNotProvided() {
        UUID id = UUID.randomUUID();
        RestaurantTable existing = table("T1", 4, true, TableStatus.Reserved);
        User manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setRoles(Role.ADMIN);
        existing.setUser(manager);

        RestaurantTableRequestDto dto = new RestaurantTableRequestDto("T3", 8, true, TableStatus.Occupied, null);
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(id, "T3", 8, "TAB-0003", true, TableStatus.Occupied, null, null, manager.getId());

        when(restaurantTableRepository.findById(id)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> {
            RestaurantTableRequestDto request = invocation.getArgument(0);
            RestaurantTable entity = invocation.getArgument(1);
            entity.setLabel(request.getLabel());
            entity.setSeats(request.getSeats());
            entity.setActive(request.getActive());
            entity.setStatus(request.getStatus());
            entity.setUser(null);
            return entity;
        }).when(restaurantTableMapper).updateEntity(dto, existing);
        when(restaurantTableRepository.count()).thenReturn(2L);
        when(restaurantTableRepository.existsByPublicCode(any())).thenReturn(false);
        when(restaurantTableRepository.save(any(RestaurantTable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restaurantTableMapper.toDto(any(RestaurantTable.class))).thenReturn(response);

        RestaurantTableResponseDto result = service.updateTable(id, dto);

        assertThat(existing.getUser()).isSameAs(manager);
        assertThat(result).isSameAs(response);
    }

    @Test
    void createTable_rejectsNonManagerUsers() {
        UUID userId = UUID.randomUUID();
        RestaurantTableRequestDto dto = new RestaurantTableRequestDto("T2", 6, true, TableStatus.Available, userId);
        RestaurantTable mapped = table("T2", 6, true, TableStatus.Available);
        User user = new User();
        user.setId(userId);
        user.setRoles(Role.CUSTOMER);

        when(restaurantTableRepository.count()).thenReturn(1L);
        when(restaurantTableRepository.existsByPublicCode(any())).thenReturn(false);
        when(restaurantTableMapper.toEntity(dto)).thenReturn(mapped);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createTable(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Table manager must have ADMIN or CASHIER role.");

        verify(restaurantTableRepository, never()).save(any());
    }

    @Test
    void deleteTable_deletesExistingTable() {
        UUID id = UUID.randomUUID();

        when(restaurantTableRepository.existsById(id)).thenReturn(true);

        service.deleteTable(id);

        verify(restaurantTableRepository).deleteById(id);
    }

    @Test
    void createTable_invalidSeats_throwsIllegalArgumentException() {
        RestaurantTableRequestDto dto = new RestaurantTableRequestDto("T1", 0, true, TableStatus.Available, null);

        assertThatThrownBy(() -> service.createTable(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Seats must be between 1 and 20.");

        verify(restaurantTableMapper, never()).toEntity(any());
    }

    private static RestaurantTable table(String label, Integer seats, Boolean active, TableStatus status) {
        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        table.setLabel(label);
        table.setSeats(seats);
        table.setActive(active);
        table.setStatus(status);
        return table;
    }
}
