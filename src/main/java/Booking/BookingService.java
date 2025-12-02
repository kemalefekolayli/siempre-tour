package Booking;

import Booking.Dto.BookingApprovalDto;
import Booking.Dto.BookingRejectionDto;
import Booking.Dto.BookingRequestDto;
import Booking.Dto.BookingResponseDto;
import Exceptions.ErrorCodes;
import Exceptions.GlobalException;
import Security.JwtHelper;
import Tours.Models.Tour;
import Tours.TourRepository;
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

    @Transactional
    public BookingResponseDto createBookingRequest(BookingRequestDto dto) {
        String userId = jwtHelper.getCurrentUserId();
        String userEmail = jwtHelper.getCurrentUserEmail();

        Tour tour = tourRepository.findById(dto.getTourId())
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));

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

    @Transactional
    public BookingResponseDto approveBooking(Long bookingId, BookingApprovalDto dto) {
        String adminId = jwtHelper.getCurrentUserId();

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
        String adminId = jwtHelper.getCurrentUserId();

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
        String userId = jwtHelper.getCurrentUserId();

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
        String userId = jwtHelper.getCurrentUserId();
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
        String userId = jwtHelper.getCurrentUserId();
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
        dto.setUserMessage(booking.getUserMessage());
        dto.setAdminNote(booking.getAdminNote());
        dto.setApprovedBy(booking.getApprovedBy());
        dto.setApprovedAt(booking.getApprovedAt());
        dto.setRejectedBy(booking.getRejectedBy());
        dto.setRejectedAt(booking.getRejectedAt());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }
}