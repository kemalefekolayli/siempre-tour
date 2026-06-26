package com.siempretour.Homepage.Dto;

import lombok.Data;

/**
 * One fully-editable card in the "En Beğenilen Lokasyonlar" section.
 * The "Keşfet" link points to template_tours_grid_page.html?country={country}.
 */
@Data
public class Section1Card {
    private String imageUrl;     // card background image
    private String title;        // e.g. "Küba"
    private String description;  // paragraph shown on hover
    private String country;      // destination key for the explore link, e.g. "Cuba"
}
