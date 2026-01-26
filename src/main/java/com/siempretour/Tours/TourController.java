package com.siempretour.Tours;

import com.siempretour.Filter.PagedResponse;
import com.siempretour.Tours.Dto.TourCreateDto;
import com.siempretour.Tours.Dto.TourFilterDto;
import com.siempretour.Tours.Dto.TourResponseDto;
import com.siempretour.Tours.Dto.TourUpdateDto;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    // ==================== CRUD Operations ====================

    @PostMapping
    public ResponseEntity<TourResponseDto> createTour(@Valid @RequestBody TourCreateDto dto) {
        log.info("Creating new tour: {}", dto.getName());
        TourResponseDto response = tourService.createTour(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{tourId}")
    public ResponseEntity<TourResponseDto> updateTour(
            @PathVariable Long tourId,
            @Valid @RequestBody TourUpdateDto dto) {
        log.info("Updating tour with ID: {}", tourId);
        TourResponseDto response = tourService.updateTour(tourId, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tourId}")
    public ResponseEntity<Void> deleteTour(@PathVariable Long tourId) {
        log.info("Deleting tour with ID: {}", tourId);
        tourService.deleteTour(tourId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tourId}")
    public ResponseEntity<TourResponseDto> getTourById(@PathVariable Long tourId) {
        TourResponseDto response = tourService.getTourById(tourId);
        return ResponseEntity.ok(response);
    }

    // ==================== Paginated List Endpoints ====================

    @GetMapping
    public ResponseEntity<PagedResponse<TourResponseDto>> getAllTours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Getting all tours - page: {}, size: {}", page, size);
        PagedResponse<TourResponseDto> response = tourService.getAllTours(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<TourResponseDto>> getActiveTours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Getting active tours - page: {}, size: {}", page, size);
        PagedResponse<TourResponseDto> response = tourService.getActiveTours(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/published")
    public ResponseEntity<PagedResponse<TourResponseDto>> getPublishedTours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("Getting published tours - page: {}, size: {}", page, size);
        PagedResponse<TourResponseDto> response = tourService.getPublishedTours(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-tours")
    public ResponseEntity<PagedResponse<TourResponseDto>> getMyTours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Getting my tours - page: {}, size: {}", page, size);
        PagedResponse<TourResponseDto> response = tourService.getMyTours(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    // ==================== Advanced Filtering Endpoint ====================

    @GetMapping("/filter")
    public ResponseEntity<PagedResponse<TourResponseDto>> filterTours(
            @RequestParam(required = false) TourStatus status,
            @RequestParam(required = false) TourCategory category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String departureCity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTo,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(required = false) String q, // search query for tour name
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Filtering tours with params - status: {}, category: {}, page: {}, size: {}",
                status, category, page, size);

        TourFilterDto filter = new TourFilterDto();
        filter.setStatus(status);
        filter.setCategory(category);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setDepartureCity(departureCity);
        filter.setStartDateFrom(startDateFrom);
        filter.setStartDateTo(startDateTo);
        filter.setMinDuration(minDuration);
        filter.setMaxDuration(maxDuration);
        filter.setSearchQuery(q);

        PagedResponse<TourResponseDto> response = tourService.filterTours(
                filter, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    // POST endpoint for complex filter operations using request body
    @PostMapping("/filter")
    public ResponseEntity<PagedResponse<TourResponseDto>> filterToursPost(
            @RequestBody TourFilterDto filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Filtering tours (POST) with filter - page: {}, size: {}", page, size);

        PagedResponse<TourResponseDto> response = tourService.filterTours(
                filter, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }
}