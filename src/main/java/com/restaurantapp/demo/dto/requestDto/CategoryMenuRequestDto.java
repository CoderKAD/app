package com.restaurantapp.demo.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMenuRequestDto {
    @NotBlank(message = "Category name is required")
    private String categoryName;

    @NotNull(message = "Active is required")
    private Boolean active;
}
