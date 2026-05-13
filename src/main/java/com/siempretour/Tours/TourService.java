package com.siempretour.Tours;

import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Filter.PagedResponse;
import com.siempretour.Filter.PaginationConstants;
import com.siempretour.Filter.TourSpecification;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Tours.Dto.*;
import com.siempretour.Tours.Models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;
    private final JwtHelper jwtHelper;

    // ==================== CREATE ====================

    @Transactional
    public TourResponseDto createTour(TourCreateDto dto) {
        Long userId = jwtHelper.getCurrentUserId();

        // Validations (only if both dates are provided)
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
            }
        }

        if (dto.getDiscountedPrice() != null && dto.getPrice() != null &&
                dto.getDiscountedPrice().compareTo(dto.getPrice()) >= 0) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        Tour tour = new Tour();
        mapCreateDtoToEntity(dto, tour);
        tour.setCreatedBy(userId);

        if (dto.getMaxParticipants() != null) {
            tour.setAvailableSeats(dto.getMaxParticipants());
        }

        Tour savedTour = tourRepository.save(tour);
        log.info("Tour created with ID: {} by user: {}", savedTour.getId(), userId);

        return mapToResponseDto(savedTour);
    }

    // ==================== UPDATE ====================

    @Transactional
    public TourResponseDto updateTour(Long tourId, TourUpdateDto dto) {
        Long userId = jwtHelper.getCurrentUserId();

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));

        // Update only non-null fields
        if (dto.getName() != null) tour.setName(dto.getName());
        if (dto.getSlug() != null) tour.setSlug(dto.getSlug());
        if (dto.getLanguage() != null) tour.setLanguage(dto.getLanguage());
        if (dto.getDestination() != null) tour.setDestination(dto.getDestination());
        if (dto.getGeneralInfo() != null) tour.setGeneralInfo(dto.getGeneralInfo());
        if (dto.getPlacesVisited() != null) tour.setPlacesVisited(dto.getPlacesVisited());
        if (dto.getWhatExpect() != null) tour.setWhatExpect(dto.getWhatExpect());
        if (dto.getMainPhoto() != null) tour.setMainPhoto(dto.getMainPhoto());
        if (dto.getImage1() != null) tour.setImage1(dto.getImage1());
        if (dto.getImage2() != null) tour.setImage2(dto.getImage2());
        if (dto.getImage3() != null) tour.setImage3(dto.getImage3());
        if (dto.getImage4() != null) tour.setImage4(dto.getImage4());
        if (dto.getImage5() != null) tour.setImage5(dto.getImage5());
        if (dto.getImage6() != null) tour.setImage6(dto.getImage6());
        if (dto.getImagealt() != null) tour.setImagealt(dto.getImagealt());
        if (dto.getPersonNumber() != null) tour.setPersonNumber(dto.getPersonNumber());
        if (dto.getDates() != null) tour.setDates(dto.getDates());
        if (dto.getMinimumAge() != null) tour.setMinimumAge(dto.getMinimumAge());
        if (dto.getMeet() != null) tour.setMeet(dto.getMeet());
        if (dto.getMap() != null) tour.setMap(dto.getMap());
        if (dto.getPrice() != null) tour.setPrice(dto.getPrice());
        if (dto.getDiscountedPrice() != null) tour.setDiscountedPrice(dto.getDiscountedPrice());
        if (dto.getDestinations() != null) tour.setDestinations(dto.getDestinations());
        if (dto.getDepartureCity() != null) tour.setDepartureCity(dto.getDepartureCity());
        if (dto.getDuration() != null) tour.setDuration(dto.getDuration());
        if (dto.getMinParticipants() != null) tour.setMinParticipants(dto.getMinParticipants());
        if (dto.getMaxParticipants() != null) {
            tour.setMaxParticipants(dto.getMaxParticipants());
            if (tour.getAvailableSeats() != null && tour.getAvailableSeats() > dto.getMaxParticipants()) {
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

        // Update day info
        if (dto.getDayInfo() != null) {
            tour.getDayInfo().clear();
            for (TourDayDto dayDto : dto.getDayInfo()) {
                TourDay day = new TourDay();
                day.setDayNumber(dayDto.getDayNumber());
                day.setTitle(dayDto.getTitle());
                day.setDescription(dayDto.getDescription());
                day.setTour(tour);
                tour.getDayInfo().add(day);
            }
        }

        // Update route
        if (dto.getRoute() != null) {
            tour.setRoute(dto.getRoute().stream()
                    .map(r -> new TourRouteStop(r.getName(), r.getCountry()))
                    .collect(Collectors.toList()));
        }

        // Update route coordinates
        if (dto.getRouteCoordinates() != null) {
            tour.setRouteCoordinates(dto.getRouteCoordinates().stream()
                    .map(rc -> new TourRouteCoordinate(rc.getName(), rc.getCountry(), rc.getLat(), rc.getLng()))
                    .collect(Collectors.toList()));
        }

        Tour updatedTour = tourRepository.save(tour);
        log.info("Tour updated with ID: {} by user: {}", tourId, userId);

        return mapToResponseDto(updatedTour);
    }

    // ==================== DELETE ====================

    @Transactional
    public void deleteTour(Long tourId) {
        Long userId = jwtHelper.getCurrentUserId();

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));

        tour.setIsActive(false);
        tour.setStatus(TourStatus.CANCELLED);
        tourRepository.save(tour);

        log.info("Tour soft deleted with ID: {} by user: {}", tourId, userId);
    }

    // ==================== GET BY ID / SLUG ====================

    public TourResponseDto getTourById(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));
        return mapToResponseDto(tour);
    }

    public TourResponseDto getTourBySlug(String slug, String language) {
        Tour tour = tourRepository.findBySlugAndLanguage(slug, language)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));
        return mapToResponseDto(tour);
    }

    // ==================== GET BY DESTINATION ====================

    public List<TourResponseDto> getToursByDestination(String destination, String language, String category) {
        List<Tour> tours;
        if (category != null && !category.isEmpty()) {
            TourCategory tourCategory = TourCategory.fromString(category);
            tours = tourRepository.findByDestinationAndLanguageAndCategory(destination, language, tourCategory);
        } else {
            tours = tourRepository.findByDestinationAndLanguage(destination, language);
        }
        return tours.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public PagedResponse<TourResponseDto> getToursByDestination(
            String destination, String language, String category, int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);

        Page<Tour> tourPage;
        if (category != null && !category.isEmpty()) {
            TourCategory tourCategory = TourCategory.fromString(category);
            tourPage = tourRepository.findByDestinationAndLanguageAndCategory(
                    destination, language, tourCategory, pageable);
        } else {
            tourPage = tourRepository.findByDestinationAndLanguage(destination, language, pageable);
        }

        return mapToPagedResponse(tourPage);
    }

    // ==================== Paginated List Endpoints ====================

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
                pageable);
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

        Page<Tour> tourPage = tourRepository.findAll(TourSpecification.withFilters(filter), pageable);

        return mapToPagedResponse(tourPage);
    }

    // ==================== Non-Paginated Methods ====================

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
                LocalDateTime.now()).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<TourResponseDto> getMyToursNonPaged() {
        Long userId = jwtHelper.getCurrentUserId();
        return tourRepository.findByCreatedBy(userId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ==================== BULK IMPORT ====================

    @Transactional
    public List<TourResponseDto> bulkImportTours(List<TourCreateDto> dtos) {
        List<TourResponseDto> results = new ArrayList<>();

        for (TourCreateDto dto : dtos) {
            Tour tour = new Tour();
            mapCreateDtoToEntity(dto, tour);
            tour.setStatus(TourStatus.PUBLISHED);
            tour.setIsActive(true);

            Tour savedTour = tourRepository.save(tour);
            results.add(mapToResponseDto(savedTour));
        }

        log.info("Bulk imported {} tours", results.size());
        return results;
    }

    // ==================== Helper Methods ====================

    private void mapCreateDtoToEntity(TourCreateDto dto, Tour tour) {
        tour.setName(dto.getName());
        tour.setSlug(dto.getSlug());
        tour.setLanguage(dto.getLanguage() != null ? dto.getLanguage() : "tr");
        tour.setDestination(dto.getDestination());
        tour.setGeneralInfo(dto.getGeneralInfo());
        tour.setPlacesVisited(dto.getPlacesVisited());
        tour.setWhatExpect(dto.getWhatExpect());
        tour.setMainPhoto(dto.getMainPhoto());
        tour.setImage1(dto.getImage1());
        tour.setImage2(dto.getImage2());
        tour.setImage3(dto.getImage3());
        tour.setImage4(dto.getImage4());
        tour.setImage5(dto.getImage5());
        tour.setImage6(dto.getImage6());
        tour.setImagealt(dto.getImagealt());
        tour.setPersonNumber(dto.getPersonNumber());
        tour.setDates(dto.getDates());
        tour.setMinimumAge(dto.getMinimumAge());
        tour.setMeet(dto.getMeet());
        tour.setMap(dto.getMap());
        tour.setPrice(dto.getPrice());
        tour.setDiscountedPrice(dto.getDiscountedPrice());
        tour.setDestinations(dto.getDestinations() != null ? dto.getDestinations() : new ArrayList<>());
        tour.setDepartureCity(dto.getDepartureCity());
        tour.setDuration(dto.getDuration());
        tour.setMinParticipants(dto.getMinParticipants());
        tour.setMaxParticipants(dto.getMaxParticipants());
        tour.setStartDate(dto.getStartDate());
        tour.setEndDate(dto.getEndDate());
        tour.setBookingDeadline(dto.getBookingDeadline());
        tour.setCategory(dto.getCategory());
        tour.setShipName(dto.getShipName());
        tour.setShipCompany(dto.getShipCompany());

        // Day info
        if (dto.getDayInfo() != null) {
            for (TourDayDto dayDto : dto.getDayInfo()) {
                TourDay day = new TourDay();
                day.setDayNumber(dayDto.getDayNumber());
                day.setTitle(dayDto.getTitle());
                day.setDescription(dayDto.getDescription());
                day.setTour(tour);
                tour.getDayInfo().add(day);
            }
        }

        // Route
        if (dto.getRoute() != null) {
            tour.setRoute(dto.getRoute().stream()
                    .map(r -> new TourRouteStop(r.getName(), r.getCountry()))
                    .collect(Collectors.toList()));
        }

        // Route coordinates
        if (dto.getRouteCoordinates() != null) {
            tour.setRouteCoordinates(dto.getRouteCoordinates().stream()
                    .map(rc -> new TourRouteCoordinate(rc.getName(), rc.getCountry(), rc.getLat(), rc.getLng()))
                    .collect(Collectors.toList()));
        }
    }

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
        dto.setSlug(tour.getSlug());
        dto.setLanguage(tour.getLanguage());
        dto.setDestination(tour.getDestination());
        dto.setGeneralInfo(tour.getGeneralInfo());
        dto.setPlacesVisited(tour.getPlacesVisited());
        dto.setWhatExpect(tour.getWhatExpect());
        dto.setMainPhoto(tour.getMainPhoto());
        dto.setImage1(tour.getImage1());
        dto.setImage2(tour.getImage2());
        dto.setImage3(tour.getImage3());
        dto.setImage4(tour.getImage4());
        dto.setImage5(tour.getImage5());
        dto.setImage6(tour.getImage6());
        dto.setImagealt(tour.getImagealt());
        dto.setPersonNumber(tour.getPersonNumber());
        dto.setDates(tour.getDates());
        dto.setMinimumAge(tour.getMinimumAge());
        dto.setMeet(tour.getMeet());
        dto.setMap(tour.getMap());
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
        dto.setCategory(tour.getCategory() != null ? tour.getCategory().getDisplayName() : null);
        dto.setStatus(tour.getStatus());
        dto.setShipName(tour.getShipName());
        dto.setShipCompany(tour.getShipCompany());
        dto.setIsActive(tour.getIsActive());
        dto.setCreatedAt(tour.getCreatedAt());
        dto.setUpdatedAt(tour.getUpdatedAt());

        // Frontend-compatible aliases
        dto.setTourName(tour.getName());
        dto.setDurationDays(tour.getDuration() != null ? String.valueOf(tour.getDuration()) : null);

        // Day info
        if (tour.getDayInfo() != null) {
            dto.setDayInfo(tour.getDayInfo().stream()
                    .map(day -> TourDayDto.builder()
                            .id(day.getId())
                            .dayNumber(day.getDayNumber())
                            .title(day.getTitle())
                            .description(day.getDescription())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Route
        if (tour.getRoute() != null) {
            dto.setRoute(tour.getRoute().stream()
                    .map(r -> TourRouteStopDto.builder()
                            .name(r.getName())
                            .country(r.getCountry())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Route coordinates
        if (tour.getRouteCoordinates() != null) {
            dto.setRouteCoordinates(tour.getRouteCoordinates().stream()
                    .map(rc -> TourRouteCoordinateDto.builder()
                            .name(rc.getName())
                            .country(rc.getCountry())
                            .lat(rc.getLat())
                            .lng(rc.getLng())
                            .build())
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}