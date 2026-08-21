package com.siempretour.Ships;

import com.siempretour.Admin.AdminImageStorageService;
import com.siempretour.Admin.Dto.AdminImageUploadResponseDto;
import com.siempretour.Ships.Dto.ShipDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ships")
@RequiredArgsConstructor
public class ShipController {

    private final ShipService shipService;
    private final AdminImageStorageService imageStorageService;

    // Public: tüm gemiler (admin listesi + genel kullanım)
    @GetMapping
    public ResponseEntity<List<ShipDto>> listShips() {
        return ResponseEntity.ok(shipService.listShips());
    }

    // Public: farklı şirket adları (datalist için)
    @GetMapping("/companies")
    public ResponseEntity<List<String>> companies() {
        return ResponseEntity.ok(shipService.companies());
    }

    // Admin: gemi görseli yükle (Cloudinary). "/{slug}"den ÖNCE tanımlı olmalı.
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminImageUploadResponseDto> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(imageStorageService.storeTourImages(files));
    }

    // Admin: toplu içe aktar/seed
    @PostMapping("/bulk-import")
    public ResponseEntity<Map<String, Integer>> bulkImport(@RequestBody List<ShipDto> ships) {
        int n = shipService.bulkImport(ships);
        return ResponseEntity.ok(Map.of("imported", n));
    }

    // Public: tek gemi
    @GetMapping("/{slug}")
    public ResponseEntity<ShipDto> getShip(@PathVariable String slug) {
        return ResponseEntity.ok(shipService.getBySlug(slug));
    }

    // Admin: yeni gemi
    @PostMapping
    public ResponseEntity<ShipDto> create(@Valid @RequestBody ShipDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipService.create(dto));
    }

    // Admin: gemi güncelle
    @PutMapping("/{slug}")
    public ResponseEntity<ShipDto> update(@PathVariable String slug, @Valid @RequestBody ShipDto dto) {
        return ResponseEntity.ok(shipService.update(slug, dto));
    }

    // Admin: gemi sil
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        shipService.delete(slug);
        return ResponseEntity.noContent().build();
    }
}
