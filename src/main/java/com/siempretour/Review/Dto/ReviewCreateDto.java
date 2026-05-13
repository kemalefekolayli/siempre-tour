package com.siempretour.Review.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewCreateDto {

    private Long tourId;

    private String destination;

    @NotBlank(message = "Guest name is required")
    @Size(max = 100)
    private String guestName;

    @NotBlank(message = "Guest email is required")
    @Email
    @Size(max = 150)
    private String guestEmail;

    @NotNull(message = "Rating is required")
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 160)
    private String title;

    @NotBlank(message = "Comment is required")
    private String comment;

    private String language = "tr";

    private LocalDate travelDate;
}
