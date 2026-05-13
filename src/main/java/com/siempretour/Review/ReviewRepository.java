package com.siempretour.Review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTourIdAndStatusAndLanguageOrderByCreatedAtDesc(Long tourId, ReviewStatus status, String language);

    List<Review> findByDestinationIgnoreCaseAndStatusAndLanguageOrderByCreatedAtDesc(String destination, ReviewStatus status, String language);

    List<Review> findByStatusOrderByCreatedAtAsc(ReviewStatus status);
}
