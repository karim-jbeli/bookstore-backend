package com.example.paymentservice.dto;

import lombok.Data;

@Data
public class PaymentStatusUpdate {
    private Long paymentId;
    private String paymentReference;
    private Long orderId;
    private String status;
    private String gatewayTransactionId;
}