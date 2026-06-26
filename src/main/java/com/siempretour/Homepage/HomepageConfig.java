package com.siempretour.Homepage;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Single-row configuration (id = 1) that drives the two editable homepage
 * sections rendered on index.html:
 *  - section1Json: "En Beğenilen Lokasyonlar" country cards (fully editable).
 *  - section2Json: "Yeni Maceralar" cascading carousel — admin-selected tour slugs.
 *
 * The payloads are stored as JSON text so the shape can evolve without schema
 * migrations (ddl-auto=update simply keeps the two TEXT columns).
 */
@Entity
@Table(name = "homepage_config")
@Data
public class HomepageConfig {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(columnDefinition = "TEXT")
    private String section1Json;

    @Column(columnDefinition = "TEXT")
    private String section2Json;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void touch() {
        if (id == null) id = SINGLETON_ID;
        updatedAt = LocalDateTime.now();
    }
}
