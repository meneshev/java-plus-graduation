package event.service;

import dto.category.CategoryDto;
import dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto);

    List<CategoryDto> getAllCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);
}