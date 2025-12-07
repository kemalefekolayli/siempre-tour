package com.siempretour.Booking;

import com.siempretour.Booking.Dto.BookingApprovalDto;
import com.siempretour.Booking.Dto.BookingRejectionDto;
import com.siempretour.Booking.Dto.BookingRequestDto;
import com.siempretour.Booking.Dto.BookingResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(@Valid @RequestBody BookingRequestDto dto) {
        log.info("Creating booking request for tour: {}", dto.getTourId());
        BookingResponseDto response = bookingService.createBookingRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<BookingResponseDto> approveBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingApprovalDto dto) {
        log.info("Approving booking with ID: {}", bookingId);
        BookingResponseDto response = bookingService.approveBooking(bookingId, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<BookingResponseDto> rejectBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingRejectionDto dto) {
        log.info("Rejecting booking with ID: {}", bookingId);
        BookingResponseDto response = bookingService.rejectBooking(bookingId, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable Long bookingId) {
        log.info("Cancelling booking with ID: {}", bookingId);
        BookingResponseDto response = bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDto> getBookingById(@PathVariable Long bookingId) {
        BookingResponseDto response = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings() {
        List<BookingResponseDto> bookings = bookingService.getMyBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<BookingResponseDto>> getPendingBookings() {
        List<BookingResponseDto> bookings = bookingService.getPendingBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/tour/{tourId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByTour(@PathVariable Long tourId) {
        List<BookingResponseDto> bookings = bookingService.getBookingsByTour(tourId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDto>> getAllBookings() {
        List<BookingResponseDto> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
}
