package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.entity.Category;


@UtilityClass
public class CategoryMapper {
    public CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName()
        );
    }


    public Category dtoToCategory(NewCategoryDto newCategoryDto) {

        Category category = new Category();
        category.setName(newCategoryDto.getName());
        return category;
    }
}