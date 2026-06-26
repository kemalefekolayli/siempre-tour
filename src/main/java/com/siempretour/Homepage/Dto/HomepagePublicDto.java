package com.siempretour.Homepage.Dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolved homepage payload consumed by index.html (public GET /api/homepage).
 * section1 cards are returned as-is; section2 slugs are resolved to live tour data.
 */
@Data
public class HomepagePublicDto {
    private List<Section1Card> section1 = new ArrayList<>();
    private List<Section2Tour> section2 = new ArrayList<>();
}
