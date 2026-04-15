package com.restaurantapp.demo.mapper;

import com.restaurantapp.demo.dto.ResponseDto.ReservationDemandResponseDto;
import com.restaurantapp.demo.dto.requestDto.ReservationDemandRequestDto;
import com.restaurantapp.demo.entity.Reservation;
import com.restaurantapp.demo.entity.ReservationDemand;
import com.restaurantapp.demo.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ReservationDemandMapper {
    @Mapping(target = "reservation", source = "reservationId", qualifiedByName = "mapReservationId")
    @Mapping(target = "user", source = "userId", qualifiedByName = "mapUserId")
    ReservationDemand toEntity(ReservationDemandRequestDto dto);

    @Mapping(target = "reservation", source = "reservationId", qualifiedByName = "mapReservationId")
    @Mapping(target = "user", source = "userId", qualifiedByName = "mapUserId")
    void updateEntity(ReservationDemandRequestDto dto, @MappingTarget ReservationDemand entity);

    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "customerId", source = "user.id")
    ReservationDemandResponseDto toDto(ReservationDemand entity);

    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "customerId", source = "user.id")
    List<ReservationDemandResponseDto> toDto(List<ReservationDemand> entity);

    @Named("mapReservationId")
    default Reservation mapReservationId(UUID reservationId) {
        if (reservationId == null) {
            return null;
        }
        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        return reservation;
    }

    @Named("mapUserId")
    default User mapUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        User user = new User();
        user.setId(userId);
        return user;
    }
}
