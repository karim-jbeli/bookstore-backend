package com.example.cartservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "total_price")
    private Double totalPrice;

    @PrePersist
    @PreUpdate
    protected void calculateTotal() {
        this.totalPrice = this.price * this.quantity;
    }
}