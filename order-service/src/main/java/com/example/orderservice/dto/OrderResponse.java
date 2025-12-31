package com.example.orderservice.dto;

import com.example.orderservice.model.Order.OrderStatus;
import com.example.orderservice.model.Order.PaymentStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userName;
    private List<OrderItemResponse> items;
    private ShippingAddressDto shippingAddress;
    private Double totalAmount;
    private Double taxAmount;
    private Double shippingCost;
    private Double finalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private String paymentReference;
    private String notes;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    @Data
    public static class ShippingAddressDto {
        private String street;
        private String city;
        private String postalCode;
        private String country;
        private String phone;
    }
}