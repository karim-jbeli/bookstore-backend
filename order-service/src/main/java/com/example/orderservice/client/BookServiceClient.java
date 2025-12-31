package com.example.orderservice.client;

import com.example.orderservice.dto.BookInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "book-service")
public interface BookServiceClient {

    @GetMapping("/{id}")
    BookInfo getBookById(@PathVariable("id") Long id);

    @PutMapping("/{id}/stock")
    void updateStock(@PathVariable("id") Long id, @RequestParam Integer quantity);
}