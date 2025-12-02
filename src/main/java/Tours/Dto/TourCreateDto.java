package Tours.Dto;


import jakarta.validation.constraints.*;
import lombok.Data;
import Tours.Models.TourCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TourCreateDto {

    @NotBlank(message = "Tour name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;


    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false, message = "Discounted price must be greater than 0")
    private BigDecimal discountedPrice;

    @NotEmpty(message = "At least one destination is required")
    private List<String> destinations = new ArrayList<>();

    @Size(max = 100)
    private String departureCity;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer duration;

    @Min(value = 1, message = "Minimum participants must be at least 1")
    private Integer minParticipants;

    @NotNull(message = "Maximum participants is required")
    @Min(value = 1, message = "Maximum participants must be at least 1")
    private Integer maxParticipants;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private LocalDateTime bookingDeadline;

    private TourCategory category;

}