package com.restaurantapp.demo.mapper;

import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import com.restaurantapp.demo.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    
    @Mapping(target = "createdBy", source = "createdById", qualifiedByName = "mapUserId")
    @Mapping(target = "updatedBy", source = "updatedById", qualifiedByName = "mapUserId")
    @Mapping(target = "tables", source = "tableIds", qualifiedByName = "mapTableIds")
    Reservation toEntity(ReservationRequestDto dto);

    @Mapping(target = "createdBy", source = "createdById", qualifiedByName = "mapUserId")
    @Mapping(target = "updatedBy", source = "updatedById", qualifiedByName = "mapUserId")
    @Mapping(target = "tables", source = "tableIds", qualifiedByName = "mapTableIds")
    void updateEntity(ReservationRequestDto dto, @MappingTarget Reservation entity);

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "tableIds", source = "tables", qualifiedByName = "mapTablesToIds")
    ReservationResponseDto toDto(Reservation entity);

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "tableIds", source = "tables", qualifiedByName = "mapTablesToIds")
    List<ReservationResponseDto> toDto(List<Reservation> entity);

    @Named("mapUserId")
    default User mapUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        User user = new User();
        user.setId(userId);
        return user;
    }

    @Named("mapTableIds")
    default List<RestaurantTable> mapTableIds(List<UUID> tableIds) {
        if (tableIds == null) {
            return null;
        }
        return tableIds.stream()
                .map(id -> {
                    RestaurantTable table = new RestaurantTable();
                    table.setId(id);
                    return table;
                })
                .toList();
    }

    @Named("mapTablesToIds")
    default List<UUID> mapTablesToIds(List<RestaurantTable> tables) {
        if (tables == null) {
            return null;
        }
        return tables.stream()
                .map(RestaurantTable::getId)
                .toList();
    }
}
