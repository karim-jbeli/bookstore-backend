package com.example.bookservice.controller;


import com.example.bookservice.dto.CategoryDTO;
import com.example.bookservice.dto.CreateCategoryRequest;
import com.example.bookservice.dto.UpdateCategoryRequest;
import com.example.bookservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints for category management")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/main")
    @Operation(summary = "Get main categories (without parent)")
    public ResponseEntity<List<CategoryDTO>> getMainCategories() {
        return ResponseEntity.ok(categoryService.getMainCategories());
    }

    @GetMapping("/{id}/subcategories")
    @Operation(summary = "Get subcategories by parent ID")
    public ResponseEntity<List<CategoryDTO>> getSubCategories(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getSubCategories(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new category (Admin only)")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category (Admin only)")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category (Admin only)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }
}