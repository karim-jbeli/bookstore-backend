package com.example.cartservice.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartResponse {
    private Long id;
    private Long userId;
    private String sessionId;
    private List<CartItemResponse> items;
    private Double totalAmount;
    private Integer itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
}