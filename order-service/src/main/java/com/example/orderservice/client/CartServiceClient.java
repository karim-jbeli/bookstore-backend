package com.example.orderservice.client;

import com.example.orderservice.dto.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "cart-service")
public interface CartServiceClient {

    @GetMapping
    CartInfo getCart(@RequestHeader("X-User-Id") Long userId);
}