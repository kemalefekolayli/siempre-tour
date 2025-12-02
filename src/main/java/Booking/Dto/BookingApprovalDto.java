package Booking.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingApprovalDto {
    @NotBlank(message = "Admin note is required")
    private String adminNote;
}