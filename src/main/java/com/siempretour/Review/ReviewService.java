package com.siempretour.Review;

import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Review.Dto.ReviewCreateDto;
import com.siempretour.Review.Dto.ReviewModerationDto;
import com.siempretour.Review.Dto.ReviewResponseDto;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.TourRepository;
import com.siempretour.User.UserEntity;
import com.siempretour.User.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TourRepository tourRepository;
    private final UserEntityRepository userEntityRepository;
    private final JwtHelper jwtHelper;

    public List<ReviewResponseDto> getApprovedReviewsByTour(Long tourId, String lang) {
        return reviewRepository.findByTourIdAndStatusAndLanguageOrderByCreatedAtDesc(
                        tourId, ReviewStatus.APPROVED, normalizeLanguage(lang))
                .stream()
                .map(this::mapToPublicDto)
                .collect(Collectors.toList());
    }

    public List<ReviewResponseDto> getApprovedReviewsByDestination(String destination, String lang) {
        return reviewRepository.findByDestinationIgnoreCaseAndStatusAndLanguageOrderByCreatedAtDesc(
                        destination, ReviewStatus.APPROVED, normalizeLanguage(lang))
                .stream()
                .map(this::mapToPublicDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponseDto createReview(ReviewCreateDto dto) {
        if (dto.getTourId() == null && isBlank(dto.getDestination())) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        Tour tour = null;
        if (dto.getTourId() != null) {
            tour = tourRepository.findById(dto.getTourId())
                    .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));
        }

        Review review = new Review();
        review.setTour(tour);
        review.setDestination(resolveDestination(dto, tour));
        review.setUser(resolveCurrentUser());
        review.setGuestName(dto.getGuestName().trim());
        review.setGuestEmail(dto.getGuestEmail().trim());
        review.setRating(dto.getRating());
        review.setTitle(trimToNull(dto.getTitle()));
        review.setComment(dto.getComment().trim());
        review.setLanguage(normalizeLanguage(dto.getLanguage()));
        review.setTravelDate(dto.getTravelDate());
        review.setStatus(ReviewStatus.PENDING);

        Review saved = reviewRepository.save(review);
        log.info("Review created as pending: {}", saved.getId());
        return mapToPublicDto(saved);
    }

    public List<ReviewModerationDto> getPendingReviews() {
        requireAdmin();
        return reviewRepository.findByStatusOrderByCreatedAtAsc(ReviewStatus.PENDING).stream()
                .map(this::mapToModerationDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewModerationDto approveReview(Long id) {
        requireAdmin();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ErrorCodes.REVIEW_COULD_NOT_BE_FOUND));
        review.setStatus(ReviewStatus.APPROVED);
        review.setApprovedAt(LocalDateTime.now());
        Review saved = reviewRepository.save(review);
        log.info("Review approved: {}", id);
        return mapToModerationDto(saved);
    }

    @Transactional
    public ReviewModerationDto rejectReview(Long id) {
        requireAdmin();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ErrorCodes.REVIEW_COULD_NOT_BE_FOUND));
        review.setStatus(ReviewStatus.REJECTED);
        review.setApprovedAt(null);
        Review saved = reviewRepository.save(review);
        log.info("Review rejected: {}", id);
        return mapToModerationDto(saved);
    }

    private void requireAdmin() {
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }
    }

    private String resolveDestination(ReviewCreateDto dto, Tour tour) {
        if (!isBlank(dto.getDestination())) return dto.getDestination().trim();
        if (tour != null && !isBlank(tour.getDestination())) return tour.getDestination();
        if (tour != null && tour.getDestinations() != null && !tour.getDestinations().isEmpty()) {
            return tour.getDestinations().get(0);
        }
        return null;
    }

    private UserEntity resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userEntityRepository.findById(userId).orElse(null);
    }

    private ReviewResponseDto mapToPublicDto(Review review) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(review.getId());
        if (review.getTour() != null) {
            dto.setTourId(review.getTour().getId());
            dto.setTourName(review.getTour().getName());
        }
        dto.setDestination(review.getDestination());
        dto.setGuestName(review.getGuestName());
        dto.setRating(review.getRating());
        dto.setTitle(review.getTitle());
        dto.setComment(review.getComment());
        dto.setLanguage(review.getLanguage());
        dto.setTravelDate(review.getTravelDate());
        dto.setStatus(review.getStatus());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        dto.setApprovedAt(review.getApprovedAt());
        return dto;
    }

    private ReviewModerationDto mapToModerationDto(Review review) {
        ReviewModerationDto dto = new ReviewModerationDto();
        dto.setId(review.getId());
        if (review.getTour() != null) {
            dto.setTourId(review.getTour().getId());
            dto.setTourName(review.getTour().getName());
        }
        if (review.getUser() != null) {
            dto.setUserId(review.getUser().getId());
        }
        dto.setDestination(review.getDestination());
        dto.setGuestName(review.getGuestName());
        dto.setGuestEmail(review.getGuestEmail());
        dto.setRating(review.getRating());
        dto.setTitle(review.getTitle());
        dto.setComment(review.getComment());
        dto.setLanguage(review.getLanguage());
        dto.setTravelDate(review.getTravelDate());
        dto.setStatus(review.getStatus());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        dto.setApprovedAt(review.getApprovedAt());
        return dto;
    }

    private String normalizeLanguage(String lang) {
        if (isBlank(lang)) return "tr";
        return lang.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) return null;
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
