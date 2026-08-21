package com.siempretour.Ships.Dto;

import lombok.Data;

@Data
public class ShipCabinDto {
    private String label;
    // Frontend "image" alan adını kullanıyor.
    private String image;
    private Boolean temsili;
}
