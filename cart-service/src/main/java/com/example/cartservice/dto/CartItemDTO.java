package com.example.cartservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long bookId;
    private String bookTitle;
    private String bookIsbn;
    private BigDecimal bookPrice;
    private String coverImageUrl;
    private Integer quantity;
    private BigDecimal subtotal;
}