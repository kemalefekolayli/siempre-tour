package com.siempretour.Review;

import com.siempretour.Tours.Models.Tour;
import com.siempretour.User.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(length = 200)
    private String destination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String guestName;

    @Email
    @Column(nullable = false, length = 150)
    private String guestEmail;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Column(length = 160)
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, length = 5)
    private String language = "tr";

    private LocalDate travelDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
