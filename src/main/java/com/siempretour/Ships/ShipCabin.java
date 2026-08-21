package com.siempretour.Ships;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class ShipCabin {

    @Column(length = 200)
    private String label;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // true ise "Temsili görsel" (o gemiye özel değil).
    private Boolean temsili;
}
