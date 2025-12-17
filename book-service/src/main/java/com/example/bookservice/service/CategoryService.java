package com.example.bookservice.service;

import com.example.bookservice.dto.CategoryDTO;
import com.example.bookservice.dto.CreateCategoryRequest;
import com.example.bookservice.dto.UpdateCategoryRequest;
import com.example.bookservice.model.Category;
import com.example.bookservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getMainCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getSubCategories(Long parentId) {
        return categoryRepository.findByParentCategoryId(parentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return convertToDTO(category);
    }

    @Transactional
    public CategoryDTO createCategory(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        if (request.getParentCategoryId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParentCategory(parentCategory);
        }

        Category savedCategory = categoryRepository.save(category);
        return convertToDTO(savedCategory);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getParentCategoryId() != null) {
            if (request.getParentCategoryId().equals(id)) {
                throw new RuntimeException("Category cannot be its own parent");
            }
            Category parentCategory = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParentCategory(parentCategory);
        }

        Category updatedCategory = categoryRepository.save(category);
        return convertToDTO(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        // Check if category has subcategories
        if (!category.getSubCategories().isEmpty()) {
            throw new RuntimeException("Cannot delete category with subcategories");
        }

        categoryRepository.delete(category);
    }

    private CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());

        if (category.getParentCategory() != null) {
            CategoryDTO parentDTO = new CategoryDTO();
            parentDTO.setId(category.getParentCategory().getId());
            parentDTO.setName(category.getParentCategory().getName());
            dto.setParentCategory(parentDTO);
        }

        return dto;
    }
}