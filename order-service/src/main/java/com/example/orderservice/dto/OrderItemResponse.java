package com.example.orderservice.dto;

import lombok.Data;

@Data
public class OrderItemResponse {
    private Long id;
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private Double price;
    private Integer quantity;
    private Double totalPrice;
}