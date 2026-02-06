package com.siempretour.Tours;

import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {

    // ==================== Non-paginated methods (for internal use) ====================

    java.util.Optional<Tour> findBySlug(String slug);

    List<Tour> findByIsActiveTrue();

    List<Tour> findByStatus(TourStatus status);

    List<Tour> findByCategory(TourCategory category);

    List<Tour> findByCreatedBy(Long userId);

    List<Tour> findByStartDateAfter(LocalDateTime date);

    List<Tour> findByIsActiveTrueAndStatusAndStartDateAfter(
            TourStatus status, LocalDateTime date);

    // ==================== Paginated methods ====================

    Page<Tour> findByIsActiveTrue(Pageable pageable);

    Page<Tour> findByStatus(TourStatus status, Pageable pageable);

    Page<Tour> findByCategory(TourCategory category, Pageable pageable);

    Page<Tour> findByCreatedBy(Long userId, Pageable pageable);

    Page<Tour> findByIsActiveTrueAndStatusAndStartDateAfter(
            TourStatus status, LocalDateTime date, Pageable pageable);

    // ==================== Advanced filtering with pagination ====================

    @Query("SELECT t FROM Tour t WHERE t.isActive = true " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:minPrice IS NULL OR t.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR t.price <= :maxPrice) " +
            "AND (:departureCity IS NULL OR LOWER(t.departureCity) LIKE LOWER(CONCAT('%', :departureCity, '%'))) " +
            "AND (:startDateFrom IS NULL OR t.startDate >= :startDateFrom) " +
            "AND (:startDateTo IS NULL OR t.startDate <= :startDateTo) " +
            "AND (:minDuration IS NULL OR t.duration >= :minDuration) " +
            "AND (:maxDuration IS NULL OR t.duration <= :maxDuration) " +
            "AND (:searchQuery IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')))")
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
            Pageable pageable
    );
}