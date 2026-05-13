package com.siempretour.Tours.Dto;


import jakarta.validation.constraints.*;
import lombok.Data;
import com.siempretour.Tours.Models.TourCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TourCreateDto {

    @NotBlank(message = "Tour name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    private String slug;

    // Language & Destination
    private String language = "tr";
    private String destination;

    // Content
    private String generalInfo;
    private String placesVisited;
    private String whatExpect;

    // Images (relative paths, stored in frontend)
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

    // Pricing
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false, message = "Discounted price must be greater than 0")
    private BigDecimal discountedPrice;

    // Destinations list
    private List<String> destinations = new ArrayList<>();

    @Size(max = 100)
    private String departureCity;

    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer duration;

    @Min(value = 1, message = "Minimum participants must be at least 1")
    private Integer minParticipants;

    @Min(value = 1, message = "Maximum participants must be at least 1")
    private Integer maxParticipants;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime bookingDeadline;

    private String shipName;
    private String shipCompany;

    private TourCategory category;

    // Day-by-day itinerary
    private List<TourDayDto> dayInfo;

    // Route
    private List<TourRouteStopDto> route;
    private List<TourRouteCoordinateDto> routeCoordinates;
}