package com.siempretour.Booking;

import com.siempretour.Booking.Dto.BookingApprovalDto;
import com.siempretour.Booking.Dto.BookingRejectionDto;
import com.siempretour.Booking.Dto.BookingRequestDto;
import com.siempretour.Booking.Dto.BookingResponseDto;
import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourDeparture;
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
        // Rezervasyon formunda e-posta girildiyse onu kullan; yoksa hesabın e-postası.
        String userEmail = (dto.getUserEmail() != null && !dto.getUserEmail().isBlank())
                ? dto.getUserEmail().trim()
                : user.getEmail();

        Tour tour;
        if (dto.getTourSlug() != null && !dto.getTourSlug().isEmpty()) {
            tour = tourRepository.findBySlug(dto.getTourSlug())
                    .orElseGet(() -> createPlaceholderTour(dto));
        } else if (dto.getTourId() != null) {
            tour = tourRepository.findById(dto.getTourId())
                    .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));
        } else {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR); // Todo: Add specific error code
        }

        // Seçilen kalkış (çoklu tarih). Varsa kontenjan/uygunluk kalkış seviyesinde
        // değerlendirilir; yoksa mevcut tur seviyesindeki mantık korunur.
        TourDeparture selectedDeparture = null;
        if (dto.getDepartureId() != null) {
            final Long departureId = dto.getDepartureId();
            selectedDeparture = tour.getDepartures().stream()
                    .filter(d -> departureId.equals(d.getId()))
                    .findFirst()
                    .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE));
            if (selectedDeparture.getAvailableSeats() != null
                    && selectedDeparture.getAvailableSeats() < dto.getNumberOfPeople()) {
                throw new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE);
            }
        } else {
            // Check if tour is bookable
            if (!tour.isBookable()) {
                throw new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE);
            }

            // Check if enough seats available
            if (tour.getAvailableSeats() < dto.getNumberOfPeople()) {
                throw new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE);
            }
        }

        Booking booking = new Booking();
        booking.setTour(tour);
        if (selectedDeparture != null) {
            booking.setDeparture(selectedDeparture);
            booking.setDepartureDate(selectedDeparture.getDepartureDate());
        }
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

    private Tour createPlaceholderTour(BookingRequestDto dto) {
        String slug = dto.getTourSlug();
        log.info("Creating placeholder tour for slug: {}", slug);
        Tour tour = new Tour();

        // Use supplied name or fallback
        String name = (dto.getTourName() != null && !dto.getTourName().isEmpty())
                ? dto.getTourName()
                : slug.replace("-", " ");

        tour.setName(name);
        tour.setSlug(slug);

        // Map optional fields
        if (dto.getTourDestination() != null && !dto.getTourDestination().isEmpty()) {
            tour.getDestinations().add(dto.getTourDestination());
        }

        if (dto.getTourPrice() != null) {
            tour.setPrice(dto.getTourPrice());
        } else {
            tour.setPrice(java.math.BigDecimal.ZERO);
        }

        if (dto.getTourDuration() != null) {
            tour.setDuration(dto.getTourDuration());
        } else {
            tour.setDuration(1);
        }

        // Category mapping
        if (dto.getTourCategory() != null) {
            try {
                // Try to match enum
                tour.setCategory(
                        com.siempretour.Tours.Models.TourCategory.valueOf(dto.getTourCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Fallback or ignore
                tour.setCategory(com.siempretour.Tours.Models.TourCategory.OTHER);
            }
        } else {
            tour.setCategory(com.siempretour.Tours.Models.TourCategory.OTHER);
        }

        // Set defaults
        tour.setAvailableSeats(20);
        tour.setMaxParticipants(20);
        tour.setStartDate(LocalDateTime.now().plusDays(30)); // 30 days from now
        tour.setEndDate(LocalDateTime.now().plusDays(30).plusDays(tour.getDuration()));
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
        TourDeparture departure = booking.getDeparture();

        if (departure != null && departure.getAvailableSeats() != null) {
            // Kontenjan seçilen kalkış seviyesinde tutuluyor.
            if (departure.getAvailableSeats() < booking.getNumberOfPeople()) {
                throw new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE);
            }
            departure.setAvailableSeats(departure.getAvailableSeats() - booking.getNumberOfPeople());
            tourRepository.save(tour); // departures cascade ile persist edilir
        } else {
            // Check if still enough seats
            if (tour.getAvailableSeats() < booking.getNumberOfPeople()) {
                throw new GlobalException(ErrorCodes.TOUR_NOT_BOOKABLE);
            }

            // Decrease available seats
            tour.decrementAvailableSeats(booking.getNumberOfPeople());
            tourRepository.save(tour);
        }

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

    // Admin: rezervasyonu kalıcı olarak siler. Rezervasyon ONAYLI ise tuttuğu
    // kontenjan (kalkış varsa kalkış seviyesinde, yoksa tur seviyesinde) geri eklenir.
    @Transactional
    public void deleteBooking(Long bookingId) {
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.RESERVATION_COULD_NOT_BE_CREATED));

        // Yalnızca ONAYLI rezervasyonlar kontenjan tutar; onları silerken iade et.
        if (booking.getStatus() == BookingStatus.APPROVED) {
            Tour tour = booking.getTour();
            TourDeparture departure = booking.getDeparture();
            if (departure != null && departure.getAvailableSeats() != null) {
                departure.setAvailableSeats(departure.getAvailableSeats() + booking.getNumberOfPeople());
                tourRepository.save(tour); // departures cascade ile persist edilir
            } else if (tour != null) {
                tour.incrementAvailableSeats(booking.getNumberOfPeople());
                tourRepository.save(tour);
            }
        }

        bookingRepository.delete(booking);
        log.info("Booking deleted: {} (status was {})", bookingId, booking.getStatus());
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

    // Admin rezervasyon araması: tur adı / kişi adı / e-posta / telefon + opsiyonel durum.
    public List<BookingResponseDto> searchBookings(String q, String status) {
        // Admin only
        if (!jwtHelper.hasRole("ADMIN")) {
            throw new GlobalException(ErrorCodes.VALIDATION_ERROR);
        }

        // LIKE pattern'i burada kur; boşsa null (sorgu "hepsi" olarak davranır).
        String normalizedQ = (q != null && !q.isBlank()) ? "%" + q.trim().toLowerCase() + "%" : null;

        BookingStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = BookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Geçersiz durum -> filtre uygulanmaz (tümü)
            }
        }

        return bookingRepository.searchBookings(normalizedQ, statusFilter).stream()
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
        dto.setTourDestination(booking.getTour().getDestination());
        dto.setTourCategory(booking.getTour().getCategory() != null ? booking.getTour().getCategory().name() : null);
        if (booking.getDeparture() != null) {
            dto.setDepartureId(booking.getDeparture().getId());
            dto.setDepartureReturnDate(booking.getDeparture().getReturnDate());
        }
        dto.setDepartureDate(booking.getDepartureDate());
        dto.setUserMessage(booking.getUserMessage());
        dto.setAdminNote(booking.getAdminNote());
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