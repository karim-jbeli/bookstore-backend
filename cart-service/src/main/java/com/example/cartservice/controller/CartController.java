package com.example.cartservice.controller;

import com.example.cartservice.dto.AddToCartRequest;
import com.example.cartservice.dto.CartDTO;
import com.example.cartservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Endpoints for shopping cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get cart by session ID")
    public ResponseEntity<CartDTO> getCart(@RequestParam String sessionId) {
        CartDTO cart = cartService.getCart(sessionId);
        if (cart == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new cart")
    public ResponseEntity<CartDTO> createCart(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(cartService.createCart(userId));
    }

    @PostMapping("/add")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartDTO> addToCart(
            @RequestParam String sessionId,
            @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItemToCart(sessionId, request));
    }

    @PutMapping("/update")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartDTO> updateCartItem(
            @RequestParam String sessionId,
            @RequestParam Long bookId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(sessionId, bookId, quantity));
    }

    @DeleteMapping("/remove")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<CartDTO> removeFromCart(
            @RequestParam String sessionId,
            @RequestParam Long bookId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(sessionId, bookId));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear cart")
    public ResponseEntity<Void> clearCart(@RequestParam String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart with user cart")
    public ResponseEntity<CartDTO> mergeCarts(
            @RequestParam String guestSessionId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(cartService.mergeCarts(guestSessionId, userId));
    }
}