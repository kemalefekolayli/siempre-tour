package com.siempretour.Tours;


import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Tours.Dto.TourCreateDto;
import com.siempretour.Tours.Dto.TourResponseDto;
import com.siempretour.Tours.Dto.TourUpdateDto;
import com.siempretour.Tours.Models.Tour;

import com.siempretour.Security.JwtHelper;

import com.siempretour.Tours.Models.TourStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;
    private final JwtHelper jwtHelper;

    @Transactional
    public TourResponseDto createTour(TourCreateDto dto) {
        Long userId = jwtHelper.getCurrentUserId();

        // Validations
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        if (dto.getDiscountedPrice() != null &&
                dto.getDiscountedPrice().compareTo(dto.getPrice()) >= 0) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        Tour tour = new Tour();
        tour.setName(dto.getName());
        tour.setPrice(dto.getPrice());
        tour.setDiscountedPrice(dto.getDiscountedPrice());
        tour.setDepartureCity(dto.getDepartureCity());
        tour.setDuration(dto.getDuration());
        tour.setMinParticipants(dto.getMinParticipants());
        tour.setMaxParticipants(dto.getMaxParticipants());
        tour.setAvailableSeats(dto.getMaxParticipants());
        tour.setStartDate(dto.getStartDate());
        tour.setEndDate(dto.getEndDate());
        tour.setBookingDeadline(dto.getBookingDeadline());
        tour.setCategory(dto.getCategory());

        Tour savedTour = tourRepository.save(tour);
        log.info("Tour created with ID: {} by user: {}", savedTour.getId(), userId);

        return mapToResponseDto(savedTour);
    }

    @Transactional
    public TourResponseDto updateTour(Long tourId, TourUpdateDto dto) {
        Long userId = jwtHelper.getCurrentUserId();

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));


        // Update only non-null fields
        if (dto.getName() != null) tour.setName(dto.getName());
        if (dto.getPrice() != null) tour.setPrice(dto.getPrice());
        if (dto.getDiscountedPrice() != null) tour.setDiscountedPrice(dto.getDiscountedPrice());
        if (dto.getDestinations() != null) tour.setDestinations(dto.getDestinations());
        if (dto.getDepartureCity() != null) tour.setDepartureCity(dto.getDepartureCity());
        if (dto.getDuration() != null) tour.setDuration(dto.getDuration());
        if (dto.getMinParticipants() != null) tour.setMinParticipants(dto.getMinParticipants());
        if (dto.getMaxParticipants() != null) {
            tour.setMaxParticipants(dto.getMaxParticipants());
            if (tour.getAvailableSeats() > dto.getMaxParticipants()) {
                tour.setAvailableSeats(dto.getMaxParticipants());
            }
        }
        if (dto.getStartDate() != null) tour.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) tour.setEndDate(dto.getEndDate());
        if (dto.getBookingDeadline() != null) tour.setBookingDeadline(dto.getBookingDeadline());
        if (dto.getCategory() != null) tour.setCategory(dto.getCategory());
        if (dto.getStatus() != null) tour.setStatus(dto.getStatus());

        Tour updatedTour = tourRepository.save(tour);
        log.info("Tour updated with ID: {} by user: {}", tourId, userId);

        return mapToResponseDto(updatedTour);
    }

    @Transactional
    public void deleteTour(Long tourId) {
        Long userId = jwtHelper.getCurrentUserId();

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));


        // Soft delete - sadece isActive'i false yap
        tour.setIsActive(false);
        tour.setStatus(TourStatus.CANCELLED);
        tourRepository.save(tour);

        log.info("Tour soft deleted with ID: {} by user: {}", tourId, userId);
    }

    public TourResponseDto getTourById(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));

        return mapToResponseDto(tour);
    }

    public List<TourResponseDto> getAllTours() {
        return tourRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<TourResponseDto> getActiveTours() {
        return tourRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<TourResponseDto> getPublishedTours() {
        return tourRepository.findByIsActiveTrueAndStatusAndStartDateAfter(
                        TourStatus.PUBLISHED,
                        LocalDateTime.now()
                ).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<TourResponseDto> getMyTours() {
        Long userId = jwtHelper.getCurrentUserId();
        return tourRepository.findByCreatedBy(userId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private TourResponseDto mapToResponseDto(Tour tour) {
        TourResponseDto dto = new TourResponseDto();
        dto.setId(tour.getId());
        dto.setName(tour.getName());

        dto.setPrice(tour.getPrice());
        dto.setDiscountedPrice(tour.getDiscountedPrice());
        dto.setDepartureCity(tour.getDepartureCity());
        dto.setDuration(tour.getDuration());
        dto.setMinParticipants(tour.getMinParticipants());
        dto.setMaxParticipants(tour.getMaxParticipants());
        dto.setAvailableSeats(tour.getAvailableSeats());
        dto.setStartDate(tour.getStartDate());
        dto.setEndDate(tour.getEndDate());
        dto.setBookingDeadline(tour.getBookingDeadline());
        dto.setCategory(tour.getCategory());
        dto.setStatus(tour.getStatus());
        dto.setIsActive(tour.getIsActive());
        dto.setCreatedAt(tour.getCreatedAt());
        dto.setUpdatedAt(tour.getUpdatedAt());
        return dto;
    }
}