package com.example.cartservice.service;

import com.example.bookservice.dto.BookDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class BookClient {

    @LoadBalanced
    private final RestTemplate restTemplate;

    public BookDTO getBookById(Long bookId) {
        return restTemplate.getForObject(
                "http://BOOK-SERVICE/api/books/" + bookId,
                BookDTO.class
        );
    }
}