package com.siempretour.Contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    @Query("SELECT c FROM ContactMessage c WHERE (CAST(:startDate AS timestamp) IS NULL OR c.createdAt >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR c.createdAt <= :endDate) ORDER BY c.createdAt DESC")
    List<ContactMessage> findAdminRequests(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM ContactMessage c WHERE (CAST(:startDate AS timestamp) IS NULL OR c.createdAt >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR c.createdAt <= :endDate) ORDER BY c.createdAt DESC")
    Page<ContactMessage> findAdminRequestsPaged(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
