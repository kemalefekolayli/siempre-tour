package com.siempretour.Ships;

import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Ships.Dto.ShipCabinDto;
import com.siempretour.Ships.Dto.ShipDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipService {

    private final ShipRepository shipRepository;
    private final JwtHelper jwtHelper;

    // ---------- Public reads ----------

    public List<ShipDto> listShips() {
        return shipRepository.findAllByOrderByCompanyAscNameAsc().stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public ShipDto getBySlug(String slug) {
        Ship ship = shipRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalException(ErrorCodes.VALIDATION_ERROR));
        return toDto(ship);
    }

    public List<String> companies() {
        return shipRepository.findAll().stream()
                .map(Ship::getCompany)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // ---------- Admin writes ----------

    @Transactional
    public ShipDto create(ShipDto dto) {
        requireAdmin();
        String slug = normalizeSlug(dto);
        if (shipRepository.existsBySlug(slug)) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }
        Ship ship = new Ship();
        ship.setSlug(slug);
        applyDto(ship, dto);
        Ship saved = shipRepository.save(ship);
        log.info("Ship created: {}", slug);
        return toDto(saved);
    }

    @Transactional
    public ShipDto update(String slug, ShipDto dto) {
        requireAdmin();
        Ship ship = shipRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalException(ErrorCodes.VALIDATION_ERROR));
        applyDto(ship, dto);
        Ship saved = shipRepository.save(ship);
        log.info("Ship updated: {}", slug);
        return toDto(saved);
    }

    @Transactional
    public void delete(String slug) {
        requireAdmin();
        Ship ship = shipRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalException(ErrorCodes.VALIDATION_ERROR));
        shipRepository.delete(ship);
        log.info("Ship deleted: {}", slug);
    }

    // Toplu içe aktarma / seed (varsa slug'a göre günceller).
    @Transactional
    public int bulkImport(List<ShipDto> dtos) {
        requireAdmin();
        int n = 0;
        for (ShipDto dto : dtos) {
            String slug = normalizeSlug(dto);
            if (slug == null) continue;
            Ship ship = shipRepository.findBySlug(slug).orElseGet(Ship::new);
            ship.setSlug(slug);
            applyDto(ship, dto);
            shipRepository.save(ship);
            n++;
        }
        log.info("Ships bulk-imported/updated: {}", n);
        return n;
    }

    // ---------- Helpers ----------

    private void requireAdmin() {
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }
    }

    private String normalizeSlug(ShipDto dto) {
        String slug = dto.getSlug();
        if (slug == null || slug.isBlank()) return null;
        return slug.trim().toLowerCase();
    }

    private void applyDto(Ship ship, ShipDto dto) {
        if (dto.getName() != null) ship.setName(dto.getName().trim());
        ship.setCompany(dto.getCompany() != null ? dto.getCompany().trim() : null);
        ship.setVideoUrl(dto.getVideoUrl() != null ? dto.getVideoUrl().trim() : null);
        if (dto.getIsActive() != null) ship.setIsActive(dto.getIsActive());

        ship.getDescription().clear();
        if (dto.getDescription() != null) {
            for (String d : dto.getDescription()) if (d != null && !d.isBlank()) ship.getDescription().add(d);
        }
        ship.getPhotos().clear();
        if (dto.getPhotos() != null) {
            for (String p : dto.getPhotos()) if (p != null && !p.isBlank()) ship.getPhotos().add(p.trim());
        }
        ship.getDecks().clear();
        if (dto.getDecks() != null) {
            for (String d : dto.getDecks()) if (d != null && !d.isBlank()) ship.getDecks().add(d.trim());
        }
        ship.getCabins().clear();
        if (dto.getCabins() != null) {
            for (ShipCabinDto c : dto.getCabins()) {
                if (c == null || c.getImage() == null || c.getImage().isBlank()) continue;
                ShipCabin cabin = new ShipCabin();
                cabin.setLabel(c.getLabel() != null ? c.getLabel().trim() : "Kabin");
                cabin.setImageUrl(c.getImage().trim());
                cabin.setTemsili(Boolean.TRUE.equals(c.getTemsili()));
                ship.getCabins().add(cabin);
            }
        }
    }

    private ShipDto toDto(Ship ship) {
        ShipDto dto = new ShipDto();
        dto.setId(ship.getId());
        dto.setSlug(ship.getSlug());
        dto.setName(ship.getName());
        dto.setCompany(ship.getCompany());
        dto.setVideoUrl(ship.getVideoUrl());
        dto.setIsActive(ship.getIsActive());
        dto.setCreatedAt(ship.getCreatedAt());
        dto.setUpdatedAt(ship.getUpdatedAt());
        dto.setDescription(new ArrayList<>(ship.getDescription()));
        dto.setPhotos(new ArrayList<>(ship.getPhotos()));
        dto.setDecks(new ArrayList<>(ship.getDecks()));
        List<ShipCabinDto> cabins = new ArrayList<>();
        for (ShipCabin c : ship.getCabins()) {
            ShipCabinDto cd = new ShipCabinDto();
            cd.setLabel(c.getLabel());
            cd.setImage(c.getImageUrl());
            cd.setTemsili(c.getTemsili());
            cabins.add(cd);
        }
        dto.setCabins(cabins);
        return dto;
    }
}
