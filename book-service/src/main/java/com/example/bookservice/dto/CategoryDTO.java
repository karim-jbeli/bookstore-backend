package com.example.bookservice.dto;


import lombok.Data;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private CategoryDTO parentCategory;
}