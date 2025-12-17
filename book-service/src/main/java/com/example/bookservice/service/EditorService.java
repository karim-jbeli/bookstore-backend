package com.example.bookservice.service;

import com.example.bookservice.dto.EditorDTO;
import com.example.bookservice.dto.CreateEditorRequest;
import com.example.bookservice.dto.UpdateEditorRequest;
import com.example.bookservice.model.Editor;
import com.example.bookservice.repository.EditorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EditorService {

    private final EditorRepository editorRepository;

    @Transactional(readOnly = true)
    public List<EditorDTO> getAllEditors() {
        return editorRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EditorDTO getEditorById(Long id) {
        Editor editor = editorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Editor not found with id: " + id));
        return convertToDTO(editor);
    }

    @Transactional
    public EditorDTO createEditor(CreateEditorRequest request) {
        Editor editor = new Editor();
        editor.setName(request.getName());
        editor.setAddress(request.getAddress());
        editor.setWebsite(request.getWebsite());
        editor.setEmail(request.getEmail());
        editor.setPhone(request.getPhone());

        Editor savedEditor = editorRepository.save(editor);
        return convertToDTO(savedEditor);
    }

    @Transactional
    public EditorDTO updateEditor(Long id, UpdateEditorRequest request) {
        Editor editor = editorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Editor not found with id: " + id));

        if (request.getName() != null) {
            editor.setName(request.getName());
        }
        if (request.getAddress() != null) {
            editor.setAddress(request.getAddress());
        }
        if (request.getWebsite() != null) {
            editor.setWebsite(request.getWebsite());
        }
        if (request.getEmail() != null) {
            editor.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            editor.setPhone(request.getPhone());
        }

        Editor updatedEditor = editorRepository.save(editor);
        return convertToDTO(updatedEditor);
    }

    @Transactional
    public void deleteEditor(Long id) {
        Editor editor = editorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Editor not found with id: " + id));
        editorRepository.delete(editor);
    }

    private EditorDTO convertToDTO(Editor editor) {
        EditorDTO dto = new EditorDTO();
        dto.setId(editor.getId());
        dto.setName(editor.getName());
        dto.setAddress(editor.getAddress());
        dto.setWebsite(editor.getWebsite());
        dto.setEmail(editor.getEmail());
        dto.setPhone(editor.getPhone());
        return dto;
    }
}