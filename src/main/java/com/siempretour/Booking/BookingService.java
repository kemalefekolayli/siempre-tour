package com.siempretour.Booking;

import com.siempretour.Booking.Dto.BookingApprovalDto;
import com.siempretour.Booking.Dto.BookingRejectionDto;
import com.siempretour.Booking.Dto.BookingRequestDto;
import com.siempretour.Booking.Dto.BookingResponseDto;
import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.TourRepository;
import com.siempretour.User.UserEntity;
import com.siempretour.User.UserEntityRepository;
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
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final JwtHelper jwtHelper;
    private final UserEntityRepository userEntityRepository;

    @Transactional
    public BookingResponseDto createBookingRequest(BookingRequestDto dto) {
        Long userId = jwtHelper.getCurrentUserId();
        UserEntity user = userEntityRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.AUTH_USER_NOT_FOUND));
        String userEmail = user.getEmail();

        Tour tour;
        if (dto.getTourSlug() != null && !dto.getTourSlug().isEmpty()) {
            tour = tourRepository.findBySlug(dto.getTourSlug())
                    .orElseGet(() -> createPlaceholderTour(dto.getTourSlug()));
        } else if (dto.getTourId() != null) {
            tour = tourRepository.findById(dto.getTourId())
                    .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));
        } else {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR); // Todo: Add specific error code
        }

        // Check if tour is bookable
        if (!tour.isBookable()) {
            throw new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE);
        }

        // Check if enough seats available
        if (tour.getAvailableSeats() < dto.getNumberOfPeople()) {
            throw new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE);
        }

        Booking booking = new Booking();
        booking.setTour(tour);
        booking.setUserId(userId);
        booking.setUserEmail(userEmail);
        booking.setUserName(dto.getUserName());
        booking.setUserPhone(dto.getUserPhone());
        booking.setNumberOfPeople(dto.getNumberOfPeople());
        booking.setUserMessage(dto.getUserMessage());
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking request created: {} for tour: {} by user: {}",
                savedBooking.getId(), tour.getId(), userId);

        return mapToResponseDto(savedBooking);
    }

    private Tour createPlaceholderTour(String slug) {
        log.info("Creating placeholder tour for slug: {}", slug);
        Tour tour = new Tour();
        // Format slug to name (e.g. "my-tour" -> "My Tour")
        String name = slug.replace("-", " ");
        // Capitalize first letters logic can be added if needed
        tour.setName(name);
        tour.setSlug(slug);

        // Set defaults
        tour.setPrice(java.math.BigDecimal.ZERO);
        tour.setAvailableSeats(20);
        tour.setMaxParticipants(20);
        tour.setDuration(1); // Default 1 day
        tour.setStartDate(LocalDateTime.now().plusDays(30)); // 30 days from now
        tour.setEndDate(LocalDateTime.now().plusDays(31));
        tour.setDepartureCity("Istanbul");
        tour.setIsActive(true);
        tour.setStatus(com.siempretour.Tours.Models.TourStatus.PUBLISHED);

        return tourRepository.save(tour);
    }

    @Transactional
    public BookingResponseDto approveBooking(Long bookingId, BookingApprovalDto dto) {
        Long adminId = jwtHelper.getCurrentUserId(); // String değil Long!

        // Check admin role
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.RESERVATION_COULD_NOT_BE_CREATED));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        Tour tour = booking.getTour();

        // Check if still enough seats
        if (tour.getAvailableSeats() < booking.getNumberOfPeople()) {
            throw new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE);
        }

        // Decrease available seats
        tour.decrementAvailableSeats(booking.getNumberOfPeople());
        tourRepository.save(tour);

        // Update booking
        booking.setStatus(BookingStatus.APPROVED);
        booking.setApprovedBy(adminId);
        booking.setApprovedAt(LocalDateTime.now());
        booking.setAdminNote(dto.getAdminNote());

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking approved: {} for tour: {} by admin: {}",
                bookingId, tour.getId(), adminId);

        return mapToResponseDto(updatedBooking);
    }

    @Transactional
    public BookingResponseDto rejectBooking(Long bookingId, BookingRejectionDto dto) {
        Long adminId = jwtHelper.getCurrentUserId();

        // Check admin role
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.RESERVATION_COULD_NOT_BE_CREATED));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectedBy(adminId);
        booking.setRejectedAt(LocalDateTime.now());
        booking.setAdminNote(dto.getRejectionReason());

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking rejected: {} for tour: {} by admin: {}",
                bookingId, booking.getTour().getId(), adminId);

        return mapToResponseDto(updatedBooking);
    }

    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId) {
        Long userId = jwtHelper.getCurrentUserId(); // String değil Long!

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.RESERVATION_COULD_NOT_BE_CREATED));

        // Check ownership
        if (!booking.getUserId().equals(userId)) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        // Can only cancel PENDING bookings
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);

        log.info("Booking cancelled: {} by user: {}", bookingId, userId);

        return mapToResponseDto(updatedBooking);
    }

    public BookingResponseDto getBookingById(Long bookingId) {
        Long userId = jwtHelper.getCurrentUserId(); // String değil Long!
        boolean isAdmin = jwtHelper.hasRole("ADMIN");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.RESERVATION_COULD_NOT_BE_CREATED));

        // Check permission: owner or admin
        if (!booking.getUserId().equals(userId) && !isAdmin) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        return mapToResponseDto(booking);
    }

    public List<BookingResponseDto> getMyBookings() {
        Long userId = jwtHelper.getCurrentUserId(); // String değil Long!
        return bookingRepository.findByUserId(userId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDto> getPendingBookings() {
        // Admin only
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        return bookingRepository.findByStatusOrderByCreatedAtAsc(BookingStatus.PENDING).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDto> getBookingsByTour(Long tourId) {
        // Admin only
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        return bookingRepository.findByTourId(tourId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDto> getAllBookings() {
        // Admin only
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        return bookingRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private BookingResponseDto mapToResponseDto(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(booking.getId());
        dto.setTourId(booking.getTour().getId());
        dto.setTourName(booking.getTour().getName());
        dto.setUserId(booking.getUserId());
        dto.setUserEmail(booking.getUserEmail());
        dto.setUserName(booking.getUserName());
        dto.setUserPhone(booking.getUserPhone());
        dto.setNumberOfPeople(booking.getNumberOfPeople());
        dto.setStatus(booking.getStatus());
        dto.setApprovedBy(booking.getApprovedBy());
        dto.setApprovedAt(booking.getApprovedAt());
        dto.setRejectedBy(booking.getRejectedBy());
        dto.setRejectedAt(booking.getRejectedAt());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }
}