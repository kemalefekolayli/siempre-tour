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
public class TourRouteStop {

    @Column(name = "stop_name")
    private String name;

    @Column(name = "stop_country")
    private String country;
}
