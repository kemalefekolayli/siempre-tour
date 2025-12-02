package Booking.Dto;

import Booking.BookingStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long id;
    private Long tourId;
    private String tourName;
    private String userId;
    private String userEmail;
    private String userName;
    private String userPhone;
    private Integer numberOfPeople;
    private BookingStatus status;
    private String userMessage;
    private String adminNote;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}