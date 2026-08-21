package com.siempretour.Booking.Dto;

import com.siempretour.Booking.BookingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long id;
    private Long tourId;
    private String tourName;
    private String tourDestination;
    private String tourCategory;
    private Long departureId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate departureDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate departureReturnDate;
    private Long userId;
    private String userEmail;
    private String userName;
    private String userPhone;
    private Integer numberOfPeople;
    private String userMessage;
    private String adminNote;
    private BookingStatus status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}