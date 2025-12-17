package com.example.bookservice.dto;

import com.example.bookservice.model.Book.BookLanguage;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateBookRequest {

    @NotBlank(message = "ISBN is required")
    @Size(min = 10, max = 20, message = "ISBN must be between 10 and 20 characters")
    private String isbn;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 500, message = "Title must be between 2 and 500 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @Min(value = 1, message = "Pages must be at least 1")
    private Integer pages;

    private LocalDate publicationDate;

    private String coverImageUrl;

    @NotNull(message = "Language is required")
    private BookLanguage language;

    private Boolean bestSeller = false;
    private Boolean newRelease = false;
    private BigDecimal weight;
    private String dimensions;

    @NotNull(message = "Author ID is required")
    private Long authorId;

    @NotNull(message = "Editor ID is required")
    private Long editorId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}