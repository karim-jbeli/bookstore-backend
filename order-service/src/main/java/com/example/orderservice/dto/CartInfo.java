package com.example.orderservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class CartInfo {
    private Long id;
    private Long userId;
    private List<CartItemInfo> items;
    private Double totalAmount;

    @Data
    public static class CartItemInfo {
        private Long bookId;
        private String title;
        private String author;
        private Double price;
        private Integer quantity;
    }
}