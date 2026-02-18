package com.siempretour.Booking.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BookingRequestDto {

    private Long tourId;

    private String tourSlug;

    // Optional fields for auto-creating tour if needed
    private String tourName;
    private String tourDestination;
    private BigDecimal tourPrice;
    private Integer tourDuration;
    private String tourCategory;

    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "At least 1 person required")
    private Integer numberOfPeople;

    @NotBlank(message = "Name is required")
    private String userName;

    @NotBlank(message = "Phone is required")
    private String userPhone;

    private String userMessage; // Özel istek/not
}