package com.siempretour.Tours.Models;



import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tours")
@Data
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @ElementCollection
    @CollectionTable(name = "tour_destinations", joinColumns = @JoinColumn(name = "tour_id"))
    @Column(name = "destination")
    @OrderColumn(name = "visit_order")
    private List<String> destinations = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TourCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TourStatus status = TourStatus.DRAFT;

    @Column(length = 100)
    private String departureCity; // uçak nerden kalkıyo

    @NotNull
    @Min(value = 1)
    private Integer duration;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = true)
    private LocalDateTime bookingDeadline;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Min(value = 1)
    private Integer minParticipants;

    @Min(value = 1)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = true)
    private Long createdBy;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Column(length = 100)
    private String shipName;     // Sadece CRUISE ise dolu olacak

    @Column(length = 100)
    private String shipCompany;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (availableSeats == null && maxParticipants != null) {
            availableSeats = maxParticipants;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isBookable() {
        return isActive &&
                status == TourStatus.PUBLISHED &&
                availableSeats > 0 &&
                LocalDateTime.now().isBefore(bookingDeadline != null ? bookingDeadline : startDate);
    }

    public void decrementAvailableSeats(int count) {
        if (availableSeats >= count) {
            availableSeats -= count;
            // Eğer koltuk kalmadıysa, status'u SOLD_OUT yap
            if (availableSeats == 0) {
                status = TourStatus.SOLD_OUT;
            }
        } else {
            throw new IllegalStateException("Not enough available seats");
        }
    }
}