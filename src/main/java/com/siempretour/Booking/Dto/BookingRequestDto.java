package com.siempretour.Booking.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequestDto {

    @NotNull(message = "Tour ID is required")
    private Long tourId;

    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "At least 1 person required")
    private Integer numberOfPeople;

    @NotBlank(message = "Name is required")
    private String userName;

    @NotBlank(message = "Phone is required")
    private String userPhone;

    private String userMessage; // Özel istek/not
}