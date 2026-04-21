package com.restaurantapp.demo.mapper;

import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    Reservation toEntity(ReservationRequestDto dto);

    void updateEntity(ReservationRequestDto dto, @MappingTarget Reservation entity);

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "tableIds", source = "tables", qualifiedByName = "mapTablesToIds")
    ReservationResponseDto toDto(Reservation entity);

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "tableIds", source = "tables", qualifiedByName = "mapTablesToIds")
    List<ReservationResponseDto> toDto(List<Reservation> entity);

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
