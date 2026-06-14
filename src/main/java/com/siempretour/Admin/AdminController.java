package com.siempretour.Admin;

import com.siempretour.Admin.Dto.*;
import com.siempretour.Filter.PagedResponse;
import com.siempretour.Tours.Dto.TourCreateDto;
import com.siempretour.Tours.Dto.TourFilterDto;
import com.siempretour.Tours.Dto.TourResponseDto;
import com.siempretour.Tours.Dto.TourUpdateDto;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import com.siempretour.Tours.TourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final TourService tourService;
    private final AdminImageStorageService imageStorageService;

    @GetMapping("/analytics/summary")
    public ResponseEntity<AdminSummaryDto> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) TourCategory category,
            @RequestParam(required = false, defaultValue = "all") String type) {
        return ResponseEntity.ok(adminService.getSummary(startDate, endDate, tourId, category, type));
    }

    @GetMapping("/analytics/requests-over-time")
    public ResponseEntity<List<AdminTimeSeriesPointDto>> getRequestsOverTime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) TourCategory category,
            @RequestParam(required = false, defaultValue = "all") String type) {
        return ResponseEntity.ok(adminService.getRequestsOverTime(startDate, endDate, tourId, category, type));
    }

    @GetMapping("/analytics/top-tours")
    public ResponseEntity<List<AdminDemandDto>> getTopTours(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) TourCategory category,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminService.getTopTours(startDate, endDate, tourId, category, limit));
    }

    @GetMapping("/analytics/top-categories")
    public ResponseEntity<List<AdminDemandDto>> getTopCategories(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) TourCategory category,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminService.getTopCategories(startDate, endDate, tourId, category, limit));
    }

    @GetMapping("/requests")
    public ResponseEntity<PagedResponse<AdminRequestDto>> getRequests(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) TourCategory category,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getRequests(startDate, endDate, tourId, category, type, page, size));
    }

    @GetMapping("/tours")
    public ResponseEntity<PagedResponse<TourResponseDto>> getTours(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) TourCategory category,
            @RequestParam(required = false) TourStatus status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        TourFilterDto filter = new TourFilterDto();
        filter.setName(q);
        filter.setSearchQuery(q);
        filter.setDestination(destination);
        filter.setLanguage(lang);
        filter.setCategory(category);
        filter.setStatus(status);
        filter.setIsActive(isActive);
        filter.setIncludeInactive(true);
        return ResponseEntity.ok(tourService.filterTours(filter, page, size, sortBy, sortDirection));
    }

    @GetMapping("/tours/{tourId}")
    public ResponseEntity<TourResponseDto> getTour(@PathVariable Long tourId) {
        return ResponseEntity.ok(tourService.getTourById(tourId));
    }

    @PostMapping("/tours")
    public ResponseEntity<TourResponseDto> createTour(@Valid @RequestBody TourCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.createTour(dto));
    }

    @PutMapping("/tours/{tourId}")
    public ResponseEntity<TourResponseDto> updateTour(@PathVariable Long tourId, @Valid @RequestBody TourUpdateDto dto) {
        return ResponseEntity.ok(tourService.updateTour(tourId, dto));
    }

    @PostMapping("/tours/{tourId}/deactivate")
    public ResponseEntity<Void> deactivateTour(@PathVariable Long tourId) {
        adminService.deactivateTour(tourId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tours/{tourId}/delete-check")
    public ResponseEntity<AdminDeleteImpactDto> getDeleteImpact(@PathVariable Long tourId) {
        return ResponseEntity.ok(adminService.getDeleteImpact(tourId));
    }

    @DeleteMapping("/tours/{tourId}/permanent")
    public ResponseEntity<Void> permanentlyDeleteTour(@PathVariable Long tourId) {
        adminService.permanentlyDeleteTour(tourId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/tours/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminImageUploadResponseDto> uploadTourImages(@RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(imageStorageService.storeTourImages(files));
    }

    @GetMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata() {
        return ResponseEntity.ok(Map.of(
                "categories", Arrays.stream(TourCategory.values()).map(category -> Map.of(
                        "name", category.name(),
                        "displayName", category.getDisplayName()
                )).toList(),
                "statuses", Arrays.stream(TourStatus.values()).map(Enum::name).toList()
        ));
    }
}
