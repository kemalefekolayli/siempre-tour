package com.siempretour.Booking.Dto;

import com.siempretour.CustomerRequest.RequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequestDto {

    @NotNull(message = "Tour ID is required")
    private Long tourId;

    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "At least 1 person required")
    private Integer numberOfPeople;

    @NotBlank(message = "Name is required")
    private String userName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String userEmail;

    @NotBlank(message = "Phone is required")
    private String userPhone;

    private String userMessage; // Özel istek/not

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status")
    private RequestStatus requestStatus;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}