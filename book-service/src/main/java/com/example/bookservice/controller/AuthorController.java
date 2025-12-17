package com.example.bookservice.controller;


import com.example.bookservice.dto.AuthorDTO;
import com.example.bookservice.dto.CreateAuthorRequest;
import com.example.bookservice.dto.UpdateAuthorRequest;
import com.example.bookservice.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books/authors")
@RequiredArgsConstructor
@Tag(name = "Authors", description = "Endpoints for author management")
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping
    @Operation(summary = "Get all authors")
    public ResponseEntity<List<AuthorDTO>> getAllAuthors() {
        return ResponseEntity.ok(authorService.getAllAuthors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get author by ID")
    public ResponseEntity<AuthorDTO> getAuthorById(@PathVariable Long id) {
        return ResponseEntity.ok(authorService.getAuthorById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new author (Admin only)")
    public ResponseEntity<AuthorDTO> createAuthor(@Valid @RequestBody CreateAuthorRequest request) {
        return ResponseEntity.ok(authorService.createAuthor(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an author (Admin only)")
    public ResponseEntity<AuthorDTO> updateAuthor(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAuthorRequest request) {
        return ResponseEntity.ok(authorService.updateAuthor(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an author (Admin only)")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok().build();
    }
}