package com.example.paymentservice.dto;


import com.example.paymentservice.model.Payment.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "User email is required")
    @Email
    private String userEmail;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Pour les cartes de crédit
    private CreditCardInfo creditCard;

    // Pour PayPal
    private String paypalEmail;

    private String description;

    @Data
    public static class CreditCardInfo {
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9]{16}$", message = "Invalid card number")
        private String cardNumber;

        @NotBlank(message = "Card holder name is required")
        private String cardHolderName;

        @NotBlank(message = "Expiry month is required")
        @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Invalid month")
        private String expiryMonth;

        @NotBlank(message = "Expiry year is required")
        @Pattern(regexp = "^[0-9]{4}$", message = "Invalid year")
        private String expiryYear;

        @NotBlank(message = "CVV is required")
        @Pattern(regexp = "^[0-9]{3,4}$", message = "Invalid CVV")
        private String cvv;
    }
}