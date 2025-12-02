package com.siempretour.Booking.Dto;

import com.siempretour.Booking.BookingStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long id;
    private Long tourId;
    private String tourName;
    private Long userId;
    private String userEmail;
    private String userName;
    private String userPhone;
    private Integer numberOfPeople;
    private BookingStatus status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}