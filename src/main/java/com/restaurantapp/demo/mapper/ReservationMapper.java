package com.restaurantapp.demo.mapper;

import com.restaurantapp.demo.dto.ResponseDto.ReservationResponseDto;
import com.restaurantapp.demo.dto.ResponseDto.ReservationSelectedTableDto;
import com.restaurantapp.demo.dto.requestDto.ReservationRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.RestaurantTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    Reservation toEntity(ReservationRequestDto dto);

    void updateEntity(ReservationRequestDto dto, @MappingTarget Reservation entity);

    @Mapping(target = "reservationId", source = "id")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "selectedTables", source = "tables", qualifiedByName = "mapTablesToSelectedTableDtos")
    ReservationResponseDto toDto(Reservation entity);

    @Mapping(target = "reservationId", source = "id")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "selectedTables", source = "tables", qualifiedByName = "mapTablesToSelectedTableDtos")
    List<ReservationResponseDto> toDto(List<Reservation> entity);

    @Named("mapTablesToSelectedTableDtos")
    default List<ReservationSelectedTableDto> mapTablesToSelectedTableDtos(List<RestaurantTable> tables) {
        if (tables == null) {
            return null;
        }
        return tables.stream()
                .map(table -> new ReservationSelectedTableDto(table.getId(), table.getPublicCode()))
                .toList();
    }
}
