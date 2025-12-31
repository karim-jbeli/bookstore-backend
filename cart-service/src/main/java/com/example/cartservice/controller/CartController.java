package com.example.cartservice.controller;


import com.example.cartservice.dto.CartItemRequest;
import com.example.cartservice.dto.CartResponse;
import com.example.cartservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
@Tag(name = "Carts", description = "Endpoints for shopping cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get or create shopping cart")
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ResponseEntity.ok(cartService.getOrCreateCart(userId, sessionId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody CartItemRequest itemRequest) {
        return ResponseEntity.ok(cartService.addItemToCart(userId, sessionId, itemRequest));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartResponse> updateItem(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(userId, sessionId, itemId, quantity));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<CartResponse> removeItem(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(userId, sessionId, itemId));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        cartService.clearCart(userId, sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge session cart with user cart")
    public ResponseEntity<Void> mergeCarts(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestHeader("X-User-Id") Long userId) {
        cartService.mergeCarts(sessionId, userId);
        return ResponseEntity.ok().build();
    }
}