package com.example.cartservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartDTO {
    private String sessionId;
    private Long userId; // null for anonymous users
    private List<CartItemDTO> items = new ArrayList<>();
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private Integer totalItems = 0;
}