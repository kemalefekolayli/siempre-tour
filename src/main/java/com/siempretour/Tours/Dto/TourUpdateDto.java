package com.siempretour.Tours.Dto;

import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TourUpdateDto {

    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
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
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal discountedPrice;

    private List<String> destinations;

    @Size(max = 100)
    private String departureCity;

    @Min(value = 1)
    private Integer duration;

    @Min(value = 1)
    private Integer minParticipants;

    @Min(value = 1)
    private Integer maxParticipants;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime bookingDeadline;

    private String shipName;
    private String shipCompany;

    private TourCategory category;
    private TourStatus status;

    // Relationships
    private List<TourDayDto> dayInfo;
    private List<TourRouteStopDto> route;
    private List<TourRouteCoordinateDto> routeCoordinates;
}