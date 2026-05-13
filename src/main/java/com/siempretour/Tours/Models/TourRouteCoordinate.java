package com.siempretour.Tours.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourRouteCoordinate {

    @Column(name = "coord_name")
    private String name;

    @Column(name = "coord_country")
    private String country;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;
}
