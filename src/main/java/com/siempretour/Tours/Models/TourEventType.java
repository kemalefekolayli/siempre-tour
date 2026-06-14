package com.siempretour.Tours.Models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TourEventType {
    NISAN_23("23 Nisan Gezisi"),
    MAYIS_1("1 Mayıs Bayramı"),
    MAYIS_19("19 Mayıs Gezisi"),
    TEMMUZ_15("15 Temmuz"),
    AGUSTOS_30("30 Ağustos"),
    EKIM_29("29 Ekim"),
    YILBASI("Yılbaşı Gezisi"),
    RAMAZAN_BAYRAMI("Ramazan Bayramı Gezisi"),
    KURBAN_BAYRAMI("Kurban Bayramı Gezisi"),
    SEMESTRE("Sömestre Gezisi"),
    TIP_BAYRAMI("Tıp Bayramı"),
    PASKALYA("Paskalya");

    private final String displayName;

    TourEventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static TourEventType fromString(String value) {
        if (value == null) return null;
        try {
            return TourEventType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {}
        for (TourEventType t : values()) {
            if (t.displayName.equalsIgnoreCase(value)) {
                return t;
            }
        }
        return null;
    }
}
