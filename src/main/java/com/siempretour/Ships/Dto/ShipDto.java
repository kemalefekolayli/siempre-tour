package com.siempretour.Ships.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ShipDto {
    private Long id;
    private String slug;
    private String name;
    private String company;
    private String videoUrl;
    private List<String> description = new ArrayList<>();
    private List<String> photos = new ArrayList<>();
    private List<String> decks = new ArrayList<>();
    private List<ShipCabinDto> cabins = new ArrayList<>();
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
