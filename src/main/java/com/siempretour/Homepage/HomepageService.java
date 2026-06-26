package com.siempretour.Homepage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siempretour.Homepage.Dto.HomepageConfigDto;
import com.siempretour.Homepage.Dto.HomepagePublicDto;
import com.siempretour.Homepage.Dto.Section1Card;
import com.siempretour.Homepage.Dto.Section2Tour;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomepageService {

    private final HomepageRepository homepageRepository;
    private final TourRepository tourRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Raw config for the admin editor (GET /api/admin/homepage). */
    @Transactional(readOnly = true)
    public HomepageConfigDto getConfig() {
        HomepageConfig entity = homepageRepository.findById(HomepageConfig.SINGLETON_ID).orElse(null);
        HomepageConfigDto dto = new HomepageConfigDto();
        if (entity == null) return dto;
        dto.setSection1(readList(entity.getSection1Json(), new TypeReference<List<Section1Card>>() {}));
        dto.setSection2(readList(entity.getSection2Json(), new TypeReference<List<String>>() {}));
        return dto;
    }

    /** Persist the admin-edited config (PUT /api/admin/homepage). */
    @Transactional
    public HomepageConfigDto saveConfig(HomepageConfigDto dto) {
        if (dto == null) dto = new HomepageConfigDto();
        HomepageConfig entity = homepageRepository.findById(HomepageConfig.SINGLETON_ID)
                .orElseGet(HomepageConfig::new);
        entity.setId(HomepageConfig.SINGLETON_ID);
        entity.setSection1Json(writeJson(dto.getSection1()));
        entity.setSection2Json(writeJson(dto.getSection2()));
        homepageRepository.save(entity);
        return getConfig();
    }

    /** Resolved payload for index.html (GET /api/homepage). */
    @Transactional(readOnly = true)
    public HomepagePublicDto getPublic(String lang) {
        String language = (lang == null || lang.isBlank()) ? "tr" : lang.toLowerCase();
        HomepageConfigDto config = getConfig();

        HomepagePublicDto out = new HomepagePublicDto();
        out.setSection1(config.getSection1());

        List<Section2Tour> resolved = new ArrayList<>();
        for (String slug : config.getSection2()) {
            if (slug == null || slug.isBlank()) continue;
            Tour tour = resolveTour(slug, language);
            if (tour == null) continue;
            Section2Tour card = new Section2Tour();
            card.setSlug(tour.getSlug());
            card.setName(tour.getName());
            card.setMainPhoto(tour.getMainPhoto());
            card.setDestination(tour.getDestination());
            resolved.add(card);
        }
        out.setSection2(resolved);
        return out;
    }

    /** Prefer the active tour in the requested language; fall back to any matching slug. */
    private Tour resolveTour(String slug, String language) {
        return tourRepository.findBySlugAndLanguageAndIsActiveTrue(slug, language)
                .or(() -> tourRepository.findBySlugAndLanguage(slug, language))
                .or(() -> tourRepository.findBySlug(slug))
                .orElse(null);
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Failed to parse homepage config JSON, returning empty list: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new ArrayList<>() : value);
        } catch (Exception e) {
            log.error("Failed to serialize homepage config", e);
            return "[]";
        }
    }
}
