package com.example.bookservice.dto;


import lombok.Data;

@Data
public class SearchRequest {
    private String title;
    private String isbn;
    private String authorName;
    private Long categoryId;
    private Boolean bestSeller;
    private Boolean newRelease;
    private Double minPrice;
    private Double maxPrice;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "title";
    private String sortDirection = "ASC";
}