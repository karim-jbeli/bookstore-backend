package com.example.cartservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cartservice.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final BookClient bookClient;

    private static final String CART_PREFIX = "cart:";
    private static final int CART_EXPIRATION_HOURS = 1;

    public CartDTO createCart(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        CartDTO cart = new CartDTO();
        cart.setSessionId(sessionId);
        cart.setUserId(userId);

        saveCart(cart);
        return cart;
    }

    public CartDTO getCart(String sessionId) {
        String key = CART_PREFIX + sessionId;
        Map<Object, Object> cartData = redisTemplate.opsForHash().entries(key);

        if (cartData.isEmpty()) {
            return null;
        }

        return convertToCartDTO(cartData);
    }

    public CartDTO addItemToCart(String sessionId, AddToCartRequest request) {
        CartDTO cart = getCart(sessionId);

        if (cart == null) {
            cart = createCart(null);
            sessionId = cart.getSessionId();
        }

        // Get book details from Book Service
        BookDTO book = bookClient.getBookById(request.getBookId());

        // Check if item already exists in cart
        boolean itemExists = false;
        for (CartItemDTO item : cart.getItems()) {
            if (item.getBookId().equals(request.getBookId())) {
                item.setQuantity(item.getQuantity() + request.getQuantity());
                item.setSubtotal(item.getBookPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                itemExists = true;
                break;
            }
        }

        // Add new item if it doesn't exist
        if (!itemExists) {
            CartItemDTO newItem = new CartItemDTO();
            newItem.setBookId(book.getId());
            newItem.setBookTitle(book.getTitle());
            newItem.setBookIsbn(book.getIsbn());
            newItem.setBookPrice(book.getPrice());
            newItem.setCoverImageUrl(book.getCoverImageUrl());
            newItem.setQuantity(request.getQuantity());
            newItem.setSubtotal(book.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));

            cart.getItems().add(newItem);
        }

        // Update totals
        updateCartTotals(cart);
        saveCart(cart);

        return cart;
    }

    public CartDTO updateCartItem(String sessionId, Long bookId, Integer quantity) {
        CartDTO cart = getCart(sessionId);

        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }

        for (CartItemDTO item : cart.getItems()) {
            if (item.getBookId().equals(bookId)) {
                if (quantity <= 0) {
                    cart.getItems().remove(item);
                } else {
                    item.setQuantity(quantity);
                    item.setSubtotal(item.getBookPrice().multiply(BigDecimal.valueOf(quantity)));
                }
                break;
            }
        }

        updateCartTotals(cart);
        saveCart(cart);

        return cart;
    }

    public CartDTO removeItemFromCart(String sessionId, Long bookId) {
        CartDTO cart = getCart(sessionId);

        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }

        cart.getItems().removeIf(item -> item.getBookId().equals(bookId));

        updateCartTotals(cart);
        saveCart(cart);

        return cart;
    }

    public void clearCart(String sessionId) {
        String key = CART_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    public CartDTO mergeCarts(String guestSessionId, Long userId) {
        CartDTO guestCart = getCart(guestSessionId);
        CartDTO userCart = getCartByUserId(userId);

        if (guestCart == null) {
            return userCart;
        }

        if (userCart == null) {
            userCart = createCart(userId);
        }

        // Merge guest cart items into user cart
        for (CartItemDTO guestItem : guestCart.getItems()) {
            boolean itemExists = false;
            for (CartItemDTO userItem : userCart.getItems()) {
                if (userItem.getBookId().equals(guestItem.getBookId())) {
                    userItem.setQuantity(userItem.getQuantity() + guestItem.getQuantity());
                    userItem.setSubtotal(userItem.getBookPrice().multiply(BigDecimal.valueOf(userItem.getQuantity())));
                    itemExists = true;
                    break;
                }
            }

            if (!itemExists) {
                userCart.getItems().add(guestItem);
            }
        }

        updateCartTotals(userCart);
        saveCart(userCart);

        // Clear guest cart
        clearCart(guestSessionId);

        return userCart;
    }

    private CartDTO getCartByUserId(Long userId) {
        // In a real implementation, you would store user-cart mapping
        // For simplicity, we'll search through all carts (not efficient for production)
        // Consider adding a user-cart index in Redis

        // This is a simplified implementation
        String pattern = CART_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        for (String key : keys) {
            Map<Object, Object> cartData = redisTemplate.opsForHash().entries(key);
            CartDTO cart = convertToCartDTO(cartData);
            if (userId.equals(cart.getUserId())) {
                cart.setSessionId(key.substring(CART_PREFIX.length()));
                return cart;
            }
        }

        return null;
    }

    private void saveCart(CartDTO cart) {
        String key = CART_PREFIX + cart.getSessionId();
        Map<String, Object> cartData = convertToMap(cart);

        redisTemplate.opsForHash().putAll(key, cartData);
        redisTemplate.expire(key, Duration.ofHours(CART_EXPIRATION_HOURS));
    }

    private void updateCartTotals(CartDTO cart) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItemDTO item : cart.getItems()) {
            totalAmount = totalAmount.add(item.getSubtotal());
            totalItems += item.getQuantity();
        }

        cart.setTotalAmount(totalAmount);
        cart.setTotalItems(totalItems);
    }

    private Map<String, Object> convertToMap(CartDTO cart) {
        try {
            String json = objectMapper.writeValueAsString(cart);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error converting cart to map", e);
        }
    }

    private CartDTO convertToCartDTO(Map<Object, Object> cartData) {
        try {
            String json = objectMapper.writeValueAsString(cartData);
            return objectMapper.readValue(json, CartDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to cart DTO", e);
        }
    }
}