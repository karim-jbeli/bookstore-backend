package com.example.bookservice.service;

import com.example.bookservice.dto.AuthorDTO;
import com.example.bookservice.dto.CreateAuthorRequest;
import com.example.bookservice.dto.UpdateAuthorRequest;
import com.example.bookservice.model.Author;
import com.example.bookservice.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    @Transactional(readOnly = true)
    public List<AuthorDTO> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AuthorDTO getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + id));
        return convertToDTO(author);
    }

    @Transactional
    public AuthorDTO createAuthor(CreateAuthorRequest request) {
        Author author = new Author();
        author.setFirstName(request.getFirstName());
        author.setLastName(request.getLastName());
        author.setBiography(request.getBiography());
        author.setBirthDate(request.getBirthDate());
        author.setNationality(request.getNationality());
        author.setPhotoUrl(request.getPhotoUrl());

        Author savedAuthor = authorRepository.save(author);
        return convertToDTO(savedAuthor);
    }

    @Transactional
    public AuthorDTO updateAuthor(Long id, UpdateAuthorRequest request) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + id));

        if (request.getFirstName() != null) {
            author.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            author.setLastName(request.getLastName());
        }
        if (request.getBiography() != null) {
            author.setBiography(request.getBiography());
        }
        if (request.getBirthDate() != null) {
            author.setBirthDate(request.getBirthDate());
        }
        if (request.getNationality() != null) {
            author.setNationality(request.getNationality());
        }
        if (request.getPhotoUrl() != null) {
            author.setPhotoUrl(request.getPhotoUrl());
        }

        Author updatedAuthor = authorRepository.save(author);
        return convertToDTO(updatedAuthor);
    }

    @Transactional
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + id));
        authorRepository.delete(author);
    }

    private AuthorDTO convertToDTO(Author author) {
        AuthorDTO dto = new AuthorDTO();
        dto.setId(author.getId());
        dto.setFirstName(author.getFirstName());
        dto.setLastName(author.getLastName());
        dto.setBiography(author.getBiography());
        dto.setBirthDate(author.getBirthDate());
        dto.setNationality(author.getNationality());
        dto.setPhotoUrl(author.getPhotoUrl());
        return dto;
    }
}