package com.example.bookservice.service;

import com.example.bookservice.dto.*;
import com.example.bookservice.model.Author;
import com.example.bookservice.model.Book;
import com.example.bookservice.model.Category;
import com.example.bookservice.model.Editor;
import com.example.bookservice.repository.AuthorRepository;
import com.example.bookservice.repository.BookRepository;
import com.example.bookservice.repository.CategoryRepository;
import com.example.bookservice.repository.EditorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final EditorRepository editorRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<BookDTO> searchBooks(SearchRequest searchRequest) {
        Pageable pageable = PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                Sort.by(Sort.Direction.fromString(searchRequest.getSortDirection()), searchRequest.getSortBy())
        );

        Page<Book> books = bookRepository.searchBooks(
                searchRequest.getTitle(),
                searchRequest.getIsbn(),
                searchRequest.getAuthorName(),
                searchRequest.getCategoryId(),
                searchRequest.getBestSeller(),
                searchRequest.getNewRelease(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                pageable
        );

        return books.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public BookDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        return convertToDTO(book);
    }

    @Transactional(readOnly = true)
    public BookDTO getBookByIsbn(String isbn) {
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new RuntimeException("Book not found with ISBN: " + isbn));
        return convertToDTO(book);
    }

    @Transactional
    public BookDTO createBook(CreateBookRequest request) {
        // Check if ISBN already exists
        if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            throw new RuntimeException("Book with ISBN " + request.getIsbn() + " already exists");
        }

        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + request.getAuthorId()));

        Editor editor = editorRepository.findById(request.getEditorId())
                .orElseThrow(() -> new RuntimeException("Editor not found with id: " + request.getEditorId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        book.setQuantity(request.getQuantity());
        book.setPages(request.getPages());
        book.setPublicationDate(request.getPublicationDate());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setLanguage(request.getLanguage());
        book.setBestSeller(request.getBestSeller());
        book.setNewRelease(request.getNewRelease());
        book.setWeight(request.getWeight());
        book.setDimensions(request.getDimensions());
        book.setAuthor(author);
        book.setEditor(editor);
        book.setCategory(category);

        Book savedBook = bookRepository.save(book);
        return convertToDTO(savedBook);
    }

    @Transactional
    public BookDTO updateBook(Long id, UpdateBookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        // Update fields if provided
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            book.setPrice(request.getPrice());
        }
        if (request.getQuantity() != null) {
            book.setQuantity(request.getQuantity());
        }
        if (request.getPages() != null) {
            book.setPages(request.getPages());
        }
        if (request.getPublicationDate() != null) {
            book.setPublicationDate(request.getPublicationDate());
        }
        if (request.getCoverImageUrl() != null) {
            book.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getLanguage() != null) {
            book.setLanguage(request.getLanguage());
        }
        if (request.getBestSeller() != null) {
            book.setBestSeller(request.getBestSeller());
        }
        if (request.getNewRelease() != null) {
            book.setNewRelease(request.getNewRelease());
        }
        if (request.getWeight() != null) {
            book.setWeight(request.getWeight());
        }
        if (request.getDimensions() != null) {
            book.setDimensions(request.getDimensions());
        }

        // Update relationships if provided
        if (request.getAuthorId() != null) {
            Author author = authorRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new RuntimeException("Author not found"));
            book.setAuthor(author);
        }
        if (request.getEditorId() != null) {
            Editor editor = editorRepository.findById(request.getEditorId())
                    .orElseThrow(() -> new RuntimeException("Editor not found"));
            book.setEditor(editor);
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            book.setCategory(category);
        }

        Book updatedBook = bookRepository.save(book);
        return convertToDTO(updatedBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        bookRepository.delete(book);
    }

    @Transactional(readOnly = true)
    public List<BookDTO> getNewReleases() {
        List<Book> books = bookRepository.findTop10ByOrderByCreatedAtDesc();
        return books.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> getBestSellers(Pageable pageable) {
        Page<Book> books = bookRepository.findByBestSellerTrue(pageable);
        return books.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> getBooksByCategory(Long categoryId, Pageable pageable) {
        Page<Book> books = bookRepository.findByCategoryId(categoryId, pageable);
        return books.map(this::convertToDTO);
    }

    private BookDTO convertToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setIsbn(book.getIsbn());
        dto.setTitle(book.getTitle());
        dto.setDescription(book.getDescription());
        dto.setPrice(book.getPrice());
        dto.setQuantity(book.getQuantity());
        dto.setPages(book.getPages());
        dto.setPublicationDate(book.getPublicationDate());
        dto.setCoverImageUrl(book.getCoverImageUrl());
        dto.setLanguage(book.getLanguage());
        dto.setBestSeller(book.getBestSeller());
        dto.setNewRelease(book.getNewRelease());
        dto.setWeight(book.getWeight());
        dto.setDimensions(book.getDimensions());

        // Convert author
        AuthorDTO authorDTO = new AuthorDTO();
        authorDTO.setId(book.getAuthor().getId());
        authorDTO.setFirstName(book.getAuthor().getFirstName());
        authorDTO.setLastName(book.getAuthor().getLastName());
        authorDTO.setBiography(book.getAuthor().getBiography());
        authorDTO.setBirthDate(book.getAuthor().getBirthDate());
        authorDTO.setNationality(book.getAuthor().getNationality());
        authorDTO.setPhotoUrl(book.getAuthor().getPhotoUrl());
        dto.setAuthor(authorDTO);

        // Convert editor
        EditorDTO editorDTO = new EditorDTO();
        editorDTO.setId(book.getEditor().getId());
        editorDTO.setName(book.getEditor().getName());
        editorDTO.setAddress(book.getEditor().getAddress());
        editorDTO.setWebsite(book.getEditor().getWebsite());
        editorDTO.setEmail(book.getEditor().getEmail());
        editorDTO.setPhone(book.getEditor().getPhone());
        dto.setEditor(editorDTO);

        // Convert category
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(book.getCategory().getId());
        categoryDTO.setName(book.getCategory().getName());
        categoryDTO.setDescription(book.getCategory().getDescription());

        if (book.getCategory().getParentCategory() != null) {
            CategoryDTO parentDTO = new CategoryDTO();
            parentDTO.setId(book.getCategory().getParentCategory().getId());
            parentDTO.setName(book.getCategory().getParentCategory().getName());
            categoryDTO.setParentCategory(parentDTO);
        }

        dto.setCategory(categoryDTO);

        return dto;
    }
}