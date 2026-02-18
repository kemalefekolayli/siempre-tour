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

    @Size(max = 500)
    private String shortDescription;

    private String detailedDescription;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal discountedPrice;

    private List<String> destinations; // maps to the list in the tour model

    @Size(max = 100)
    private String departureCity;

    @Min(value = 1)
    private Integer duration;

    @Min(value = 1)
    private Integer minParticipants;

    private String shipName;
    private String shipCompany;

    @Min(value = 1)
    private Integer maxParticipants;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime bookingDeadline;

    private TourCategory category;
    private TourStatus status;

}