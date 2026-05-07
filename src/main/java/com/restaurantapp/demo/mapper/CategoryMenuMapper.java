package com.restaurantapp.demo.mapper;

import com.restaurantapp.demo.dto.ResponseDto.CategoryMenuResponseDto;
import com.restaurantapp.demo.dto.requestDto.CategoryMenuRequestDto;
import com.restaurantapp.demo.entity.CategoryMenu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMenuMapper {
    @Mapping(target = "sortOrder", ignore = true)
    CategoryMenu toEntity(CategoryMenuRequestDto dto);

    @Mapping(target = "sortOrder", ignore = true)
    void updateEntity(CategoryMenuRequestDto dto, @MappingTarget CategoryMenu entity);

    CategoryMenuResponseDto toDto(CategoryMenu entity);

    List<CategoryMenuResponseDto> toDto(List<CategoryMenu> entity);
}
