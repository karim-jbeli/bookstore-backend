package com.example.orderservice.dto;

import lombok.Data;

@Data
public class BookInfo {
    private Long id;
    private String title;
    private String author;
    private Double price;
    private Integer stock;
    private String isbn;
}