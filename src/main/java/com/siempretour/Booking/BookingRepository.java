package com.siempretour.Booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByTourId(Long tourId);

    long countByTourId(Long tourId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByTourIdAndStatus(Long tourId, BookingStatus status);

    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE (CAST(:startDate AS timestamp) IS NULL OR b.createdAt >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR b.createdAt <= :endDate) " +
            "AND (CAST(:tourId AS long) IS NULL OR b.tour.id = :tourId) " +
            "AND (CAST(:category AS string) IS NULL OR b.tour.category = :category) " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findAdminRequests(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("tourId") Long tourId,
            @Param("category") com.siempretour.Tours.Models.TourCategory category);

    @Query("SELECT b FROM Booking b WHERE (CAST(:startDate AS timestamp) IS NULL OR b.createdAt >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR b.createdAt <= :endDate) " +
            "AND (CAST(:tourId AS long) IS NULL OR b.tour.id = :tourId) " +
            "AND (CAST(:category AS string) IS NULL OR b.tour.category = :category) " +
            "ORDER BY b.createdAt DESC")
    Page<Booking> findAdminRequestsPaged(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("tourId") Long tourId,
            @Param("category") com.siempretour.Tours.Models.TourCategory category,
            Pageable pageable);
}
