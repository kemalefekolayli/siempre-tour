package com.siempretour.Homepage.Dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Raw, admin-editable homepage configuration (used by GET/PUT /api/admin/homepage).
 * section2 holds the ordered tour slugs the admin selected for the cascading carousel.
 */
@Data
public class HomepageConfigDto {
    private List<Section1Card> section1 = new ArrayList<>();
    private List<String> section2 = new ArrayList<>();
}
