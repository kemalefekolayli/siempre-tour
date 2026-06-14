package com.siempretour.Admin.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRequestDto {
    private Long id;
    private String type;
    private Long tourId;
    private String tourName;
    private String category;
    private String destination;
    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;
    private Integer numberOfPeople;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
