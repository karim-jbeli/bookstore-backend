package com.example.bookservice.controller;


import com.example.bookservice.dto.*;
import com.example.bookservice.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Endpoints for book management")
public class BookController {

    private final BookService bookService;

    @PostMapping("/search")
    @Operation(summary = "Search books with multiple criteria")
    public ResponseEntity<Page<BookDTO>> searchBooks(@RequestBody SearchRequest searchRequest) {
        return ResponseEntity.ok(bookService.searchBooks(searchRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<BookDTO> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "Get book by ISBN")
    public ResponseEntity<BookDTO> getBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbn));
    }

    @GetMapping("/new-releases")
    @Operation(summary = "Get new releases")
    public ResponseEntity<List<BookDTO>> getNewReleases() {
        return ResponseEntity.ok(bookService.getNewReleases());
    }

    @GetMapping("/best-sellers")
    @Operation(summary = "Get best sellers")
    public ResponseEntity<Page<BookDTO>> getBestSellers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookService.getBestSellers(pageable));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get books by category")
    public ResponseEntity<Page<BookDTO>> getBooksByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookService.getBooksByCategory(categoryId, pageable));
    }

    @PostMapping
    @Operation(summary = "Create a new book (Admin only)")
    public ResponseEntity<BookDTO> createBook(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.ok(bookService.createBook(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a book (Admin only)")
    public ResponseEntity<BookDTO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book (Admin only)")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok().build();
    }
}