package com.example.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "User email is required")
    private String userEmail;

    @NotBlank(message = "User name is required")
    private String userName;

    @Valid
    @NotNull(message = "Shipping address is required")
    private ShippingAddressDto shippingAddress;

    private String paymentMethod;

    private String notes;

    @Data
    public static class ShippingAddressDto {
        @NotBlank(message = "Street is required")
        private String street;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "Postal code is required")
        private String postalCode;

        @NotBlank(message = "Country is required")
        private String country;

        @NotBlank(message = "Phone is required")
        private String phone;
    }
}