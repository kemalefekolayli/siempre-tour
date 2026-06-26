package com.siempretour.Homepage.Dto;

import lombok.Data;

/**
 * A resolved tour card for the cascading carousel (public GET /api/homepage).
 * Image + name come straight from the selected tour; the card links to its
 * detail page via the slug.
 */
@Data
public class Section2Tour {
    private String slug;
    private String name;
    private String mainPhoto;
    private String destination;
}
