package com.siempretour.Tours;

import com.siempretour.Tours.Dto.TourCreateDto;
import com.siempretour.Tours.Dto.TourResponseDto;
import com.siempretour.Tours.Dto.TourUpdateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

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

    @GetMapping
    public ResponseEntity<List<TourResponseDto>> getAllTours() {
        List<TourResponseDto> tours = tourService.getAllTours();
        return ResponseEntity.ok(tours);
    }

    @GetMapping("/active")
    public ResponseEntity<List<TourResponseDto>> getActiveTours() {
        List<TourResponseDto> tours = tourService.getActiveTours();
        return ResponseEntity.ok(tours);
    }

    @GetMapping("/published")
    public ResponseEntity<List<TourResponseDto>> getPublishedTours() {
        List<TourResponseDto> tours = tourService.getPublishedTours();
        return ResponseEntity.ok(tours);
    }

    @GetMapping("/my-tours")
    public ResponseEntity<List<TourResponseDto>> getMyTours() {
        List<TourResponseDto> tours = tourService.getMyTours();
        return ResponseEntity.ok(tours);
    }
}