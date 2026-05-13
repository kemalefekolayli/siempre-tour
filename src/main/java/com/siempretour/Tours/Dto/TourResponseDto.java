package com.siempretour.Tours.Dto;


import com.siempretour.Tours.Models.TourStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TourResponseDto {
    private Long id;
    private String name;
    private String slug;

    // Language & Destination
    private String language;
    private String destination;

    // Content
    private String generalInfo;
    private String placesVisited;
    private String whatExpect;

    // Images
    private String mainPhoto;
    private String image1;
    private String image2;
    private String image3;
    private String image4;
    private String image5;
    private String image6;
    private String imagealt;

    // Nullable future-use
    private String personNumber;
    private String dates;
    private String minimumAge;
    private String meet;
    private String map;

    // Existing fields
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private List<String> destinations;
    private String departureCity;
    private Integer duration;
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer availableSeats;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime bookingDeadline;
    private String category;
    private TourStatus status;
    private String shipName;
    private String shipCompany;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;

    // Relationships
    private List<TourDayDto> dayInfo;
    private List<TourRouteStopDto> route;
    private List<TourRouteCoordinateDto> routeCoordinates;

    // Frontend-compatible aliases
    private String tourName;      // alias for name
    private String durationDays;  // alias for duration (as string)
}