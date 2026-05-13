package com.siempretour.Review;

import com.siempretour.Review.Dto.ReviewCreateDto;
import com.siempretour.Review.Dto.ReviewModerationDto;
import com.siempretour.Review.Dto.ReviewResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/by-tour/{tourId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByTour(
            @PathVariable Long tourId,
            @RequestParam(defaultValue = "tr") String lang) {
        return ResponseEntity.ok(reviewService.getApprovedReviewsByTour(tourId, lang));
    }

    @GetMapping("/by-destination")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByDestination(
            @RequestParam String destination,
            @RequestParam(defaultValue = "tr") String lang) {
        return ResponseEntity.ok(reviewService.getApprovedReviewsByDestination(destination, lang));
    }

    @PostMapping
    public ResponseEntity<ReviewResponseDto> createReview(@Valid @RequestBody ReviewCreateDto dto) {
        log.info("Creating review for tour: {} destination: {}", dto.getTourId(), dto.getDestination());
        ReviewResponseDto response = reviewService.createReview(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ReviewModerationDto>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ReviewModerationDto> approveReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ReviewModerationDto> rejectReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.rejectReview(id));
    }
}
