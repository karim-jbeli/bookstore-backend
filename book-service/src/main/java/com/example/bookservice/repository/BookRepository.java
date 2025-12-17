package com.example.bookservice.repository;

import com.example.bookservice.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Book> findByAuthorFirstNameContainingOrAuthorLastNameContaining(String firstName, String lastName, Pageable pageable);

    Page<Book> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Book> findByBestSellerTrue(Pageable pageable);

    Page<Book> findByNewReleaseTrue(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE " +
            "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:isbn IS NULL OR b.isbn = :isbn) AND " +
            "(:authorName IS NULL OR " +
            "LOWER(CONCAT(b.author.firstName, ' ', b.author.lastName)) LIKE LOWER(CONCAT('%', :authorName, '%'))) AND " +
            "(:categoryId IS NULL OR b.category.id = :categoryId) AND " +
            "(:bestSeller IS NULL OR b.bestSeller = :bestSeller) AND " +
            "(:newRelease IS NULL OR b.newRelease = :newRelease) AND " +
            "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR b.price <= :maxPrice)")
    Page<Book> searchBooks(@Param("title") String title,
                           @Param("isbn") String isbn,
                           @Param("authorName") String authorName,
                           @Param("categoryId") Long categoryId,
                           @Param("bestSeller") Boolean bestSeller,
                           @Param("newRelease") Boolean newRelease,
                           @Param("minPrice") Double minPrice,
                           @Param("maxPrice") Double maxPrice,
                           Pageable pageable);

    List<Book> findTop10ByOrderByCreatedAtDesc();
}