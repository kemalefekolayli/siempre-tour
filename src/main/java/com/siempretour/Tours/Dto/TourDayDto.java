package com.siempretour.Tours.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourDayDto {
    private Long id;
    private Integer dayNumber;
    private String title;
    private String description;
}
