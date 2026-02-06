package com.siempretour.Tours;


import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Filter.PagedResponse;
import com.siempretour.Filter.PaginationConstants;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Tours.Dto.TourCreateDto;

import com.siempretour.Tours.Dto.TourFilterDto;
import com.siempretour.Tours.Dto.TourResponseDto;
import com.siempretour.Tours.Dto.TourUpdateDto;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        tour.setDestinations(dto.getDestinations());         // BUG FIX: destinations eklendi
        tour.setDepartureCity(dto.getDepartureCity());
        tour.setDuration(dto.getDuration());
        tour.setMinParticipants(dto.getMinParticipants());
        tour.setMaxParticipants(dto.getMaxParticipants());
        tour.setAvailableSeats(dto.getMaxParticipants());
        tour.setStartDate(dto.getStartDate());
        tour.setEndDate(dto.getEndDate());
        tour.setBookingDeadline(dto.getBookingDeadline());
        tour.setCategory(dto.getCategory());
        tour.setShipName(dto.getShipName());                 // BUG FIX: shipName eklendi
        tour.setShipCompany(dto.getShipCompany());           // BUG FIX: shipCompany eklendi
        tour.setCreatedBy(userId);

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
        if (dto.getShipName() != null) tour.setShipName(dto.getShipName());
        if (dto.getShipCompany() != null) tour.setShipCompany(dto.getShipCompany());

        Tour updatedTour = tourRepository.save(tour);
        log.info("Tour updated with ID: {} by user: {}", tourId, userId);

        return mapToResponseDto(updatedTour);
    }

    @Transactional
    public void deleteTour(Long tourId) {
        Long userId = jwtHelper.getCurrentUserId();

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));

        // Soft delete
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

    // ==================== Paginated Methods ====================

    public PagedResponse<TourResponseDto> getAllTours(int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<Tour> tourPage = tourRepository.findAll(pageable);
        return mapToPagedResponse(tourPage);
    }

    public PagedResponse<TourResponseDto> getActiveTours(int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<Tour> tourPage = tourRepository.findByIsActiveTrue(pageable);
        return mapToPagedResponse(tourPage);
    }

    public PagedResponse<TourResponseDto> getPublishedTours(int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<Tour> tourPage = tourRepository.findByIsActiveTrueAndStatusAndStartDateAfter(
                TourStatus.PUBLISHED,
                LocalDateTime.now(),
                pageable
        );
        return mapToPagedResponse(tourPage);
    }

    public PagedResponse<TourResponseDto> getMyTours(int page, int size, String sortBy, String sortDirection) {
        Long userId = jwtHelper.getCurrentUserId();
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<Tour> tourPage = tourRepository.findByCreatedBy(userId, pageable);
        return mapToPagedResponse(tourPage);
    }

    public PagedResponse<TourResponseDto> filterTours(TourFilterDto filter, int page, int size,
                                                      String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);

        Page<Tour> tourPage = tourRepository.findWithFilters(
                filter.getStatus(),
                filter.getCategory(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getDepartureCity(),
                filter.getStartDateFrom(),
                filter.getStartDateTo(),
                filter.getMinDuration(),
                filter.getMaxDuration(),
                filter.getSearchQuery(),
                pageable
        );

        return mapToPagedResponse(tourPage);
    }

    // ==================== Non-Paginated Methods (backwards compatibility) ====================

    public List<TourResponseDto> getAllToursNonPaged() {
        return tourRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<TourResponseDto> getActiveToursNonPaged() {
        return tourRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<TourResponseDto> getPublishedToursNonPaged() {
        return tourRepository.findByIsActiveTrueAndStatusAndStartDateAfter(
                        TourStatus.PUBLISHED,
                        LocalDateTime.now()
                ).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<TourResponseDto> getMyToursNonPaged() {
        Long userId = jwtHelper.getCurrentUserId();
        return tourRepository.findByCreatedBy(userId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        int normalizedPage = PaginationConstants.normalizePageNumber(page);
        int normalizedSize = PaginationConstants.normalizePageSize(size);

        Sort sort = Sort.by(sortBy != null ? sortBy : "createdAt");
        sort = "asc".equalsIgnoreCase(sortDirection) ? sort.ascending() : sort.descending();

        return PageRequest.of(normalizedPage, normalizedSize, sort);
    }

    private PagedResponse<TourResponseDto> mapToPagedResponse(Page<Tour> tourPage) {
        List<TourResponseDto> content = tourPage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return PagedResponse.<TourResponseDto>builder()
                .content(content)
                .page(tourPage.getNumber())
                .size(tourPage.getSize())
                .totalElements(tourPage.getTotalElements())
                .totalPages(tourPage.getTotalPages())
                .first(tourPage.isFirst())
                .last(tourPage.isLast())
                .hasNext(tourPage.hasNext())
                .hasPrevious(tourPage.hasPrevious())
                .build();
    }

    private TourResponseDto mapToResponseDto(Tour tour) {
        TourResponseDto dto = new TourResponseDto();
        dto.setId(tour.getId());
        dto.setName(tour.getName());
        dto.setDestinations(tour.getDestinations());
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
        dto.setShipName(tour.getShipName());
        dto.setShipCompany(tour.getShipCompany());
        dto.setIsActive(tour.getIsActive());
        dto.setCreatedAt(tour.getCreatedAt());
        dto.setUpdatedAt(tour.getUpdatedAt());
        return dto;
    }
}