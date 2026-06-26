package com.siempretour.Tours;

import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long>, JpaSpecificationExecutor<Tour> {

    Optional<Tour> findBySlug(String slug);

    Optional<Tour> findBySlugAndLanguage(String slug, String language);

    Optional<Tour> findBySlugAndLanguageAndIsActiveTrue(String slug, String language);

    List<Tour> findByIsActiveTrue();

    List<Tour> findByStatus(TourStatus status);

    List<Tour> findByCategory(TourCategory category);

    List<Tour> findByCreatedBy(Long userId);

    List<Tour> findByStartDateAfter(LocalDateTime date);

    List<Tour> findByIsActiveTrueAndStatusAndStartDateAfter(
            TourStatus status, LocalDateTime date);

    List<Tour> findByDestinationAndLanguage(String destination, String language);

    List<Tour> findByIsActiveTrueAndDestinationAndLanguage(String destination, String language);

    List<Tour> findByDestinationAndLanguageAndCategory(String destination, String language, TourCategory category);

    List<Tour> findByIsActiveTrueAndDestinationAndLanguageAndCategory(String destination, String language, TourCategory category);

    Page<Tour> findByIsActiveTrue(Pageable pageable);

    Page<Tour> findByStatus(TourStatus status, Pageable pageable);

    Page<Tour> findByCategory(TourCategory category, Pageable pageable);

    Page<Tour> findByCreatedBy(Long userId, Pageable pageable);

    Page<Tour> findByIsActiveTrueAndStatusAndStartDateAfter(
            TourStatus status, LocalDateTime date, Pageable pageable);

    Page<Tour> findByDestinationAndLanguage(String destination, String language, Pageable pageable);

    Page<Tour> findByIsActiveTrueAndDestinationAndLanguage(String destination, String language, Pageable pageable);

    Page<Tour> findByDestinationAndLanguageAndCategory(
            String destination, String language, TourCategory category, Pageable pageable);

    Page<Tour> findByIsActiveTrueAndDestinationAndLanguageAndCategory(
            String destination, String language, TourCategory category, Pageable pageable);

    /**
     * Free-text search used by the chat assistant (function calling).
     * Matches active + published tours whose name, destination or visited
     * places contain the keyword. An empty keyword matches everything
     * (useful for "upcoming tours"). Optional language filter; pass null for all.
     * Postgres orders ASC with NULLS LAST, so upcoming tours come first.
     */
    @Query("SELECT t FROM Tour t WHERE t.isActive = true " +
            "AND t.status = com.siempretour.Tours.Models.TourStatus.PUBLISHED " +
            "AND (CAST(:lang AS string) IS NULL OR t.language = :lang) " +
            "AND ( LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "   OR LOWER(COALESCE(t.destination, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "   OR LOWER(COALESCE(t.placesVisited, '')) LIKE LOWER(CONCAT('%', :q, '%')) ) " +
            "ORDER BY t.startDate ASC")
    List<Tour> searchForChat(@Param("q") String q, @Param("lang") String lang, Pageable pageable);

    @Query("SELECT t FROM Tour t WHERE t.isActive = true " +
            "AND (CAST(:status AS string) IS NULL OR t.status = :status) " +
            "AND (CAST(:category AS string) IS NULL OR t.category = :category) " +
            "AND (CAST(:minPrice AS bigdecimal) IS NULL OR t.price >= :minPrice) " +
            "AND (CAST(:maxPrice AS bigdecimal) IS NULL OR t.price <= :maxPrice) " +
            "AND (CAST(:departureCity AS string) IS NULL OR LOWER(t.departureCity) LIKE LOWER(CONCAT('%', :departureCity, '%'))) " +
            "AND (CAST(:startDateFrom AS timestamp) IS NULL OR t.startDate >= :startDateFrom) " +
            "AND (CAST(:startDateTo AS timestamp) IS NULL OR t.startDate <= :startDateTo) " +
            "AND (CAST(:minDuration AS integer) IS NULL OR t.duration >= :minDuration) " +
            "AND (CAST(:maxDuration AS integer) IS NULL OR t.duration <= :maxDuration) " +
            "AND (CAST(:searchQuery AS string) IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "AND (CAST(:language AS string) IS NULL OR t.language = :language) " +
            "AND (CAST(:destination AS string) IS NULL OR t.destination = :destination)")
    Page<Tour> findWithFilters(
            @Param("status") TourStatus status,
            @Param("category") TourCategory category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("departureCity") String departureCity,
            @Param("startDateFrom") LocalDateTime startDateFrom,
            @Param("startDateTo") LocalDateTime startDateTo,
            @Param("minDuration") Integer minDuration,
            @Param("maxDuration") Integer maxDuration,
            @Param("searchQuery") String searchQuery,
            @Param("language") String language,
            @Param("destination") String destination,
            Pageable pageable
    );
}
