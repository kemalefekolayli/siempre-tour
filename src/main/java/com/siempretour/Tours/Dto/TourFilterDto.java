package com.siempretour.Tours.Dto;


import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TourFilterDto {

    // Text search
    private String name;                    // Partial match on tour name
    private String departureCity;           // Partial match on departure city
    private String destination;             // Search within destinations list

    // Category & Status
    private TourCategory category;
    private TourStatus status;
    private List<TourCategory> categories;  // Filter by multiple categories
    private List<TourStatus> statuses;      // Filter by multiple statuses

    // Price range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean hasDiscount;            // Filter tours with discounted price

    // Duration range
    private Integer minDuration;
    private Integer maxDuration;

    // Date range
    private LocalDateTime startDateFrom;
    private LocalDateTime startDateTo;
    private LocalDateTime endDateFrom;
    private LocalDateTime endDateTo;

    // Availability
    private Integer minAvailableSeats;
    private Integer maxAvailableSeats;
    private Boolean hasAvailability;        // Tours with availableSeats > 0

    // Participants
    private Integer minParticipants;
    private Integer maxParticipants;

    // Cruise-specific
    private String shipName;
    private String shipCompany;

    // Status flags
    private Boolean isActive;
    private Boolean isBookable;             // Custom: active, published, has seats, before deadline

    // Sorting
    private String sortBy;                  // Field to sort by
    private String sortDirection;           // ASC or DESC (default: ASC)

    // Pagination
    private Integer page;                   // Page number (0-indexed)
    private Integer size;                   // Page size (default: 10)

    private String searchQuery; // Search by tour name
}