package com.example.cartservice.dto;

import lombok.Data;

@Data
public class AddToCartRequest {
    private Long bookId;
    private Integer quantity;
}