package com.siempretour.Tours.Models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bir turun tek bir kalkış (sefer) tarihi. Aynı tur birden fazla kez, farklı
 * tarihlerde yapılabildiği için Tour ile @OneToMany ilişkisi kurulur.
 * Her kalkışın kendi fiyatı / kontenjanı olabilir (null ise tur seviyesindeki
 * değere düşülür).
 */
@Entity
@Table(name = "tour_departures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourDeparture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate departureDate;

    private LocalDate returnDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    private Integer maxSeats;

    private Integer availableSeats;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    @JsonBackReference("tour-departures")
    private Tour tour;
}
