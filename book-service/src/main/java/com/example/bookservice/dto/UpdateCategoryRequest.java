package com.example.bookservice.dto;

import lombok.Data;

@Data
public class UpdateCategoryRequest {
    private String name;
    private String description;
    private Long parentCategoryId;
}