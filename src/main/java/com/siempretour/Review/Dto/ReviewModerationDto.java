package com.siempretour.Review.Dto;

import com.siempretour.Review.ReviewStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReviewModerationDto {
    private Long id;
    private Long tourId;
    private String tourName;
    private String destination;
    private Long userId;
    private String guestName;
    private String guestEmail;
    private Integer rating;
    private String title;
    private String comment;
    private String language;
    private LocalDate travelDate;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
}
