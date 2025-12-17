package com.example.bookservice.dto;


import com.example.bookservice.model.Book.BookLanguage;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateBookRequest {
    private String title;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private Integer pages;
    private LocalDate publicationDate;
    private String coverImageUrl;
    private BookLanguage language;
    private Boolean bestSeller;
    private Boolean newRelease;
    private BigDecimal weight;
    private String dimensions;
    private Long authorId;
    private Long editorId;
    private Long categoryId;
}