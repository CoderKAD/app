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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantTableMapper restaurantTableMapper;

    @InjectMocks
    private RestaurantTableService restaurantTableService;

    @Test
    void getAllTables_returnsMappedTables() {
        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        table.setLabel("T1");
        table.setSeats(4);
        table.setPublicCode("TAB-0001");
        table.setActive(true);
        table.setStatus(TableStatus.Available);
        RestaurantTableResponseDto response = new RestaurantTableResponseDto(table.getId(), table.getLabel(), table.getSeats(), table.getPublicCode(), table.getActive(), table.getStatus(), null, null, null);

        when(restaurantTableRepository.findAll()).thenReturn(List.of(table));
        when(restaurantTableMapper.toDto(List.of(table))).thenReturn(List.of(response));

        List<RestaurantTableResponseDto> result = restaurantTableService.getAllTables();

        assertThat(result).containsExactly(response);
    }

    @Test
    void createTable_success_generatesPublicCodeAndAssignsUser() {
        UUID userId = UUID.randomUUID();
        RestaurantTableRequestDto dto = new RestaurantTableRequestDto("T1", 4, true, TableStatus.Available, userId);
        RestaurantTable entity = new RestaurantTable();
        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        user.setPasswordHash("hash");
        user.setEmail("john@example.com");
        user.setRoles(Role.CUSTOMER);
        RestaurantTable saved = new RestaurantTable();
        saved.setId(UUID.randomUUID());
        saved.setLabel("T1");
        saved.setSeats(4);
        saved.setPublicCode("TAB-0001");
        saved.setActive(true);
        saved.setStatus(TableStatus.Available);
        saved.setUser(user);
        RestaurantTableResponseDto expected = new RestaurantTableResponseDto(saved.getId(), saved.getLabel(), saved.getSeats(), saved.getPublicCode(), saved.getActive(), saved.getStatus(), null, null, userId);

        when(restaurantTableMapper.toEntity(dto)).thenReturn(entity);
        when(restaurantTableRepository.count()).thenReturn(0L);
        when(restaurantTableRepository.existsByPublicCode("TAB-0001")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantTableRepository.save(entity)).thenReturn(saved);
        when(restaurantTableMapper.toDto(saved)).thenReturn(expected);

        RestaurantTableResponseDto result = restaurantTableService.createTable(dto);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<RestaurantTable> captor = ArgumentCaptor.forClass(RestaurantTable.class);
        verify(restaurantTableRepository).save(captor.capture());
        assertThat(captor.getValue().getPublicCode()).isEqualTo("TAB-0001");
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void updateTable_success_clearsUserWhenMissing() {
        UUID tableId = UUID.randomUUID();
        RestaurantTableRequestDto dto = new RestaurantTableRequestDto("T2", 6, true, TableStatus.Available, null);
        RestaurantTable existing = new RestaurantTable();
        existing.setId(tableId);
        existing.setLabel("T1");
        existing.setSeats(4);
        existing.setActive(true);
        existing.setStatus(TableStatus.Available);
        RestaurantTable saved = new RestaurantTable();
        saved.setId(tableId);
        saved.setLabel("T2");
        saved.setSeats(6);
        saved.setPublicCode("TAB-0001");
        saved.setActive(true);
        saved.setStatus(TableStatus.Available);
        RestaurantTableResponseDto expected = new RestaurantTableResponseDto(tableId, "T2", 6, "TAB-0001", true, TableStatus.Available, null, null, null);

        when(restaurantTableRepository.findById(tableId)).thenReturn(Optional.of(existing));
        when(restaurantTableRepository.count()).thenReturn(0L);
        when(restaurantTableRepository.existsByPublicCode("TAB-0001")).thenReturn(false);
        doNothing().when(restaurantTableMapper).updateEntity(dto, existing);
        when(restaurantTableRepository.save(existing)).thenReturn(saved);
        when(restaurantTableMapper.toDto(saved)).thenReturn(expected);

        RestaurantTableResponseDto result = restaurantTableService.updateTable(tableId, dto);

        assertThat(result).isSameAs(expected);
        assertThat(existing.getPublicCode()).isEqualTo("TAB-0001");
        assertThat(existing.getUser()).isNull();
    }

    @Test
    void deleteTable_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(restaurantTableRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> restaurantTableService.deleteTable(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("RestaurantTable not found");

        verify(restaurantTableRepository, never()).deleteById(any());
    }
}
