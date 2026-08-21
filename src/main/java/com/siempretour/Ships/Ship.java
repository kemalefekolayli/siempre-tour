package com.siempretour.Ships;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ships")
@Data
public class Ship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // URL anahtarı (template_ship_detail_page.html?ship=<slug>)
    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(length = 150)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String videoUrl;

    // Açıklama paragrafları (sırayla)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ship_descriptions", joinColumns = @JoinColumn(name = "ship_id"))
    @OrderColumn(name = "line_order")
    @Column(name = "line", columnDefinition = "TEXT")
    private List<String> description = new ArrayList<>();

    // Galeri (dış cephe) foto URL'leri
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ship_photos", joinColumns = @JoinColumn(name = "ship_id"))
    @OrderColumn(name = "photo_order")
    @Column(name = "url", length = 500)
    private List<String> photos = new ArrayList<>();

    // Güverte planı görselleri
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ship_decks", joinColumns = @JoinColumn(name = "ship_id"))
    @OrderColumn(name = "deck_order")
    @Column(name = "url", length = 500)
    private List<String> decks = new ArrayList<>();

    // Kabinler (label + görsel + temsili mi)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ship_cabins", joinColumns = @JoinColumn(name = "ship_id"))
    @OrderColumn(name = "cabin_order")
    private List<ShipCabin> cabins = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
