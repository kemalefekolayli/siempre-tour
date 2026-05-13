package com.siempretour.Tours.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourRouteCoordinateDto {
    private String name;
    private String country;
    private Double lat;
    private Double lng;
}
