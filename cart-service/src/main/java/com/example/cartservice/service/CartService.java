package com.example.cartservice.service;

import com.example.cartservice.client.BookServiceClient;
import com.example.cartservice.dto.*;
import com.example.cartservice.model.Cart;
import com.example.cartservice.model.CartItem;
import com.example.cartservice.repository.CartRepository;
import com.example.cartservice.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookServiceClient bookServiceClient;

    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public CartResponse getOrCreateCart(Long userId, String sessionId) {
        Cart cart;

        if (userId != null) {
            cart = cartRepository.findByUserId(userId)
                    .orElseGet(() -> createUserCart(userId));
        } else {
            cart = cartRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createSessionCart(sessionId));
        }

        return mapToCartResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public CartResponse addItemToCart(Long userId, String sessionId, CartItemRequest itemRequest) {
        Cart cart = getOrCreateCartEntity(userId, sessionId);

        // Vérifier la disponibilité du livre
        BookInfo bookInfo = bookServiceClient.getBookById(itemRequest.getBookId());
        if (bookInfo == null) {
            throw new RuntimeException("Book not found with id: " + itemRequest.getBookId());
        }

        if (bookInfo.getStock() < itemRequest.getQuantity()) {
            throw new RuntimeException("Insufficient stock for book: " + bookInfo.getTitle());
        }

        // Vérifier si l'article existe déjà dans le panier
        CartItem existingItem = cartItemRepository
                .findByCartIdAndBookId(cart.getId(), itemRequest.getBookId())
                .orElse(null);

        if (existingItem != null) {
            // Mettre à jour la quantité
            existingItem.setQuantity(existingItem.getQuantity() + itemRequest.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            // Ajouter un nouvel article
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setBookId(bookInfo.getId());
            newItem.setTitle(bookInfo.getTitle());
            newItem.setAuthor(bookInfo.getAuthor());
            newItem.setPrice(bookInfo.getPrice());
            newItem.setQuantity(itemRequest.getQuantity());

            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setExpiresAt(LocalDateTime.now().plusHours(2));
        cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public CartResponse updateCartItem(Long userId, String sessionId, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCartEntity(userId, sessionId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Item does not belong to this cart");
        }

        // Vérifier le stock
        BookInfo bookInfo = bookServiceClient.getBookById(item.getBookId());
        if (bookInfo.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        cart.calculateTotals();
        cart.setExpiresAt(LocalDateTime.now().plusHours(2));
        cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public CartResponse removeItemFromCart(Long userId, String sessionId, Long itemId) {
        Cart cart = getOrCreateCartEntity(userId, sessionId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Item does not belong to this cart");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);

        cart.setExpiresAt(LocalDateTime.now().plusHours(2));
        cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public void clearCart(Long userId, String sessionId) {
        Cart cart = getOrCreateCartEntity(userId, sessionId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cart.calculateTotals();
        cartRepository.save(cart);
    }

    @Transactional
    public void mergeCarts(String sessionId, Long userId) {
        Cart sessionCart = cartRepository.findBySessionId(sessionId).orElse(null);
        Cart userCart = cartRepository.findByUserId(userId).orElse(null);

        if (sessionCart != null) {
            if (userCart == null) {
                // Convertir le panier de session en panier utilisateur
                sessionCart.setUserId(userId);
                sessionCart.setSessionId(null);
                cartRepository.save(sessionCart);
            } else {
                // Fusionner les deux paniers
                mergeCartItems(sessionCart, userCart);
                cartRepository.delete(sessionCart);
            }
        }
    }

    @Cacheable(value = "carts", key = "#userId")
    public CartResponse getCart(Long userId, String sessionId) {
        Cart cart = getOrCreateCartEntity(userId, sessionId);
        return mapToCartResponse(cart);
    }

    @Transactional
    public void cleanupExpiredCarts() {
        LocalDateTime now = LocalDateTime.now();
        List<Cart> expiredCarts = cartRepository.findExpiredCarts(now);

        for (Cart cart : expiredCarts) {
            cartItemRepository.deleteByCartId(cart.getId());
            cartRepository.delete(cart);
            log.info("Cleaned up expired cart: {}", cart.getId());
        }
    }

    private Cart getOrCreateCartEntity(Long userId, String sessionId) {
        if (userId != null) {
            return cartRepository.findByUserId(userId)
                    .orElseGet(() -> createUserCart(userId));
        } else {
            return cartRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createSessionCart(sessionId));
        }
    }

    private Cart createUserCart(Long userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setSessionId(null);
        return cartRepository.save(cart);
    }

    private Cart createSessionCart(String sessionId) {
        Cart cart = new Cart();
        cart.setUserId(null);
        cart.setSessionId(sessionId != null ? sessionId : generateSessionId());
        return cartRepository.save(cart);
    }

    private void mergeCartItems(Cart source, Cart target) {
        for (CartItem sourceItem : source.getItems()) {
            CartItem existingItem = cartItemRepository
                    .findByCartIdAndBookId(target.getId(), sourceItem.getBookId())
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + sourceItem.getQuantity());
                cartItemRepository.save(existingItem);
            } else {
                sourceItem.setCart(target);
                target.addItem(sourceItem);
                cartItemRepository.save(sourceItem);
            }
        }

        target.calculateTotals();
        cartRepository.save(target);
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private CartResponse mapToCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setUserId(cart.getUserId());
        response.setSessionId(cart.getSessionId());
        response.setTotalAmount(cart.getTotalAmount());
        response.setItemCount(cart.getItemCount());
        response.setCreatedAt(cart.getCreatedAt());
        response.setUpdatedAt(cart.getUpdatedAt());
        response.setExpiresAt(cart.getExpiresAt());

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setBookId(item.getBookId());
        response.setTitle(item.getTitle());
        response.setAuthor(item.getAuthor());
        response.setPrice(item.getPrice());
        response.setQuantity(item.getQuantity());
        response.setTotalPrice(item.getTotalPrice());
        return response;
    }
}