package com.siempretour.Tours.Models;


import com.fasterxml.jackson.annotation.JsonManagedReference;
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

    @Column(unique = false) // slug + language combo is unique, not slug alone
    private String slug;

    // ==================== Language & Destination ====================

    @Column(nullable = false, length = 5)
    private String language = "tr"; // "tr" or "en"

    @Column(length = 200)
    private String destination; // Country name, e eoggy "Albania", "Japan"

    // ==================== Content Fields (from frontend JSON) ====================

    @Column(columnDefinition = "TEXT")
    private String generalInfo; // HTML description

    @Column(columnDefinition = "TEXT")
    private String placesVisited; // Places summary text

    @Column(columnDefinition = "TEXT")
    private String whatExpect; // Nullable, future use

    // ==================== Image Fields ====================

    @Column(length = 500)
    private String mainPhoto;

    @Column(length = 500)
    private String image1;

    @Column(length = 500)
    private String image2;

    @Column(length = 500)
    private String image3;

    @Column(length = 500)
    private String image4;

    @Column(length = 500)
    private String image5;

    @Column(length = 500)
    private String image6;

    @Column(length = 500)
    private String imagealt;

    // ==================== Nullable Future-Use Fields ====================

    @Column(length = 100)
    private String personNumber;

    @Column(length = 255)
    private String dates;

    @Column(length = 50)
    private String minimumAge;

    @Column(length = 500)
    private String meet;

    @Column(columnDefinition = "TEXT")
    private String map;

    // ==================== Existing Fields ====================

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
    private String departureCity;

    @Min(value = 1)
    private Integer duration;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LocalDateTime bookingDeadline;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Min(value = 1)
    private Integer minParticipants;

    @Min(value = 1)
    private Integer maxParticipants;

    private Integer availableSeats;

    @Column(nullable = false)
    private Boolean isActive = true;

    private Long createdBy;

    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Column(length = 100)
    private String shipName;

    @Column(length = 100)
    private String shipCompany;

    // ==================== Relationships ====================

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dayNumber ASC")
    @JsonManagedReference
    private List<TourDay> dayInfo = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tour_routes", joinColumns = @JoinColumn(name = "tour_id"))
    @OrderColumn(name = "route_order")
    private List<TourRouteStop> route = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tour_route_coordinates", joinColumns = @JoinColumn(name = "tour_id"))
    @OrderColumn(name = "coord_order")
    private List<TourRouteCoordinate> routeCoordinates = new ArrayList<>();

    // ==================== Lifecycle Hooks ====================

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

    // ==================== Business Logic ====================

    public boolean isBookable() {
        return isActive &&
                status == TourStatus.PUBLISHED &&
                availableSeats != null &&
                availableSeats > 0 &&
                LocalDateTime.now().isBefore(bookingDeadline != null ? bookingDeadline : (startDate != null ? startDate : LocalDateTime.MAX));
    }

    public void decrementAvailableSeats(int count) {
        if (availableSeats != null && availableSeats >= count) {
            availableSeats -= count;
            if (availableSeats == 0) {
                status = TourStatus.SOLD_OUT;
            }
        } else {
            throw new IllegalStateException("Not enough available seats");
        }
    }

    // ==================== Helper for TourDay management ====================

    public void setDayInfoFromList(List<TourDay> days) {
        this.dayInfo.clear();
        if (days != null) {
            for (TourDay day : days) {
                day.setTour(this);
                this.dayInfo.add(day);
            }
        }
    }
}