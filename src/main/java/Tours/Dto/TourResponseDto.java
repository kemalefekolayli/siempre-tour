package Tours.Dto;


import Tours.Models.TourCategory;
import Tours.Models.TourStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TourResponseDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private List<String> destinations;
    private String departureCity;
    private Integer duration;
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer availableSeats;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime bookingDeadline;
    private TourCategory category;
    private TourStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;

}