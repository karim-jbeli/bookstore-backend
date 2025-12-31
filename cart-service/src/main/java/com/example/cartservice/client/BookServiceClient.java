package com.example.cartservice.client;

import com.example.cartservice.dto.BookInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service")
public interface BookServiceClient {

    @GetMapping("/{id}")
    BookInfo getBookById(@PathVariable("id") Long id);

    @GetMapping("/{id}/stock")
    Integer getBookStock(@PathVariable("id") Long id);
}