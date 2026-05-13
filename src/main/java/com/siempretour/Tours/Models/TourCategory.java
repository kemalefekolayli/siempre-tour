package com.siempretour.Tours.Models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TourCategory {
    CULTURE("Culture"),
    FAMILY("Family"),
    WILDLIFE("Wildlife"),
    WALKING("Walking"),
    ADVENTURE("Adventure"),
    NATURE("Nature"),
    HISTORICAL("Historical"),
    FOOD_AND_WINE("Food & Wine"),
    BEACH("Beach"),
    CITY_TOUR("City Tour"),
    SAFARI("Safari"),
    CRUISE("Ship/Cruise"),
    SHIP("Ship"),
    RELIGIOUS("Religious"),
    SKIING("Skiing"),
    EXPLORER("Explorer"),
    CYCLING("Cycling"),
    EXPEDITION("Expedition"),
    RAIL("Rail"),
    POLAR("Polar"),
    LUXURY("Luxury"),
    YOGA_WELLNESS("Yoga&Wellness"),
    HORSE_RIDING("Horse Riding"),
    DIVING("Diving"),
    HONEYMOON("Honeymoon"),
    OTHER("Other");

    private final String displayName;

    TourCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static TourCategory fromString(String value) {
        if (value == null) return null;
        // Try exact enum name first (e.g., "CULTURE")
        try {
            return TourCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {}
        // Try display name match (e.g., "Culture", "Ship/Cruise")
        for (TourCategory cat : values()) {
            if (cat.displayName.equalsIgnoreCase(value)) {
                return cat;
            }
        }
        return OTHER;
    }
}
