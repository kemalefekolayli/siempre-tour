package Booking.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingRejectionDto {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}