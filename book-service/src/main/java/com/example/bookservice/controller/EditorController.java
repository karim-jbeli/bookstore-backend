package com.example.bookservice.controller;

import com.example.bookservice.dto.EditorDTO;
import com.example.bookservice.dto.CreateEditorRequest;
import com.example.bookservice.dto.UpdateEditorRequest;
import com.example.bookservice.service.EditorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books/editors")
@RequiredArgsConstructor
@Tag(name = "Editors", description = "Endpoints for editor management")
public class EditorController {

    private final EditorService editorService;

    @GetMapping
    @Operation(summary = "Get all editors")
    public ResponseEntity<List<EditorDTO>> getAllEditors() {
        return ResponseEntity.ok(editorService.getAllEditors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get editor by ID")
    public ResponseEntity<EditorDTO> getEditorById(@PathVariable Long id) {
        return ResponseEntity.ok(editorService.getEditorById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new editor (Admin only)")
    public ResponseEntity<EditorDTO> createEditor(@Valid @RequestBody CreateEditorRequest request) {
        return ResponseEntity.ok(editorService.createEditor(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an editor (Admin only)")
    public ResponseEntity<EditorDTO> updateEditor(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEditorRequest request) {
        return ResponseEntity.ok(editorService.updateEditor(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an editor (Admin only)")
    public ResponseEntity<Void> deleteEditor(@PathVariable Long id) {
        editorService.deleteEditor(id);
        return ResponseEntity.ok().build();
    }
}