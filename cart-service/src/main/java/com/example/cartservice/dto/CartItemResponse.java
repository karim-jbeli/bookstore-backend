package com.example.cartservice.dto;


import lombok.Data;

@Data
public class CartItemResponse {
    private Long id;
    private Long bookId;
    private String title;
    private String author;
    private Double price;
    private Integer quantity;
    private Double totalPrice;
}