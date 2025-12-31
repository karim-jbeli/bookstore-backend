package com.example.paymentservice.dto;

import com.example.paymentservice.model.Payment.PaymentMethod;
import com.example.paymentservice.model.Payment.PaymentStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private String paymentReference;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private Double amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String cardLastFour;
    private String cardBrand;
    private String currency;
    private String description;
    private String gatewayTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
}