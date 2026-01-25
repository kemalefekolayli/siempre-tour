package com.siempretour.Booking;

import com.siempretour.Booking.Booking;
import com.siempretour.Booking.BookingRepository;
import com.siempretour.Booking.BookingService;
import com.siempretour.Booking.BookingStatus;
import com.siempretour.Booking.Dto.BookingApprovalDto;
import com.siempretour.Booking.Dto.BookingRejectionDto;
import com.siempretour.Booking.Dto.BookingRequestDto;
import com.siempretour.Booking.Dto.BookingResponseDto;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import com.siempretour.Tours.TourRepository;
import com.siempretour.User.UserEntity;
import com.siempretour.User.UserEntityRepository;
import com.siempretour.User.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserEntityRepository userEntityRepository;

    @Mock
    private JwtHelper jwtHelper;

    @InjectMocks
    private BookingService bookingService;

    private Tour bookableTour;
    private UserEntity user;
    private UserEntity admin;
    private Booking pendingBooking;

    @BeforeEach
    void setUp() {
        bookableTour = createBookableTour();
        user = createUser(1L, "user@test.com", UserRole.USER);
        admin = createUser(2L, "admin@test.com", UserRole.ADMIN);
        pendingBooking = createPendingBooking();
    }

    // ==================== CREATE BOOKING TESTS ====================

    @Nested
    @DisplayName("createBookingRequest")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully")
        void createBooking_ValidRequest_ShouldSucceed() {
            BookingRequestDto dto = createValidBookingDto();

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(userEntityRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tourRepository.findById(1L)).thenReturn(Optional.of(bookableTour));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                b.setId(1L);
                return b;
            });

            BookingResponseDto result = bookingService.createBookingRequest(dto);

            assertNotNull(result);
            assertEquals(BookingStatus.PENDING, result.getStatus());
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw when user not found")
        void createBooking_UserNotFound_ShouldThrow() {
            BookingRequestDto dto = createValidBookingDto();

            when(jwtHelper.getCurrentUserId()).thenReturn(999L);
            when(userEntityRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(GlobalException.class, () -> bookingService.createBookingRequest(dto));
        }

        @Test
        @DisplayName("Should throw when tour not found")
        void createBooking_TourNotFound_ShouldThrow() {
            BookingRequestDto dto = createValidBookingDto();
            dto.setTourId(999L);

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(userEntityRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tourRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(GlobalException.class, () -> bookingService.createBookingRequest(dto));
        }

        @Test
        @DisplayName("Should throw when tour is not bookable")
        void createBooking_TourNotBookable_ShouldThrow() {
            BookingRequestDto dto = createValidBookingDto();
            bookableTour.setStatus(TourStatus.DRAFT);

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(userEntityRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tourRepository.findById(1L)).thenReturn(Optional.of(bookableTour));

            assertThrows(GlobalException.class, () -> bookingService.createBookingRequest(dto));
        }

        @Test
        @DisplayName("Should throw when not enough seats")
        void createBooking_NotEnoughSeats_ShouldThrow() {
            BookingRequestDto dto = createValidBookingDto();
            dto.setNumberOfPeople(50);
            bookableTour.setAvailableSeats(10);

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(userEntityRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tourRepository.findById(1L)).thenReturn(Optional.of(bookableTour));

            assertThrows(GlobalException.class, () -> bookingService.createBookingRequest(dto));
        }

        @Test
        @DisplayName("Should throw when tour is inactive")
        void createBooking_InactiveTour_ShouldThrow() {
            BookingRequestDto dto = createValidBookingDto();
            bookableTour.setIsActive(false);

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(userEntityRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tourRepository.findById(1L)).thenReturn(Optional.of(bookableTour));

            assertThrows(GlobalException.class, () -> bookingService.createBookingRequest(dto));
        }

        @Test
        @DisplayName("Should throw when booking deadline passed")
        void createBooking_DeadlinePassed_ShouldThrow() {
            BookingRequestDto dto = createValidBookingDto();
            bookableTour.setBookingDeadline(LocalDateTime.now().minusDays(1));

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(userEntityRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tourRepository.findById(1L)).thenReturn(Optional.of(bookableTour));

            assertThrows(GlobalException.class, () -> bookingService.createBookingRequest(dto));
        }
    }

    // ==================== APPROVE BOOKING TESTS ====================

    @Nested
    @DisplayName("approveBooking")
    class ApproveBookingTests {

        @Test
        @DisplayName("Should approve booking successfully")
        void approveBooking_Valid_ShouldSucceed() {
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");

            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));
            when(tourRepository.save(any(Tour.class))).thenReturn(bookableTour);
            when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);

            BookingResponseDto result = bookingService.approveBooking(1L, dto);

            assertEquals(BookingStatus.APPROVED, result.getStatus());
            verify(tourRepository, times(1)).save(any(Tour.class));
        }

        @Test
        @DisplayName("Should throw when not admin")
        void approveBooking_NotAdmin_ShouldThrow() {
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(false);

            assertThrows(GlobalException.class, () -> bookingService.approveBooking(1L, dto));
        }

        @Test
        @DisplayName("Should throw when booking not found")
        void approveBooking_NotFound_ShouldThrow() {
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");

            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(GlobalException.class, () -> bookingService.approveBooking(999L, dto));
        }

        @Test
        @DisplayName("Should throw when booking not pending")
        void approveBooking_NotPending_ShouldThrow() {
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");
            pendingBooking.setStatus(BookingStatus.APPROVED);

            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));

            assertThrows(GlobalException.class, () -> bookingService.approveBooking(1L, dto));
        }

        @Test
        @DisplayName("Should throw when not enough seats left")
        void approveBooking_NotEnoughSeats_ShouldThrow() {
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");
            pendingBooking.setNumberOfPeople(50);
            bookableTour.setAvailableSeats(10);

            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));

            assertThrows(GlobalException.class, () -> bookingService.approveBooking(1L, dto));
        }

        @Test
        @DisplayName("Should decrement available seats on approval")
        void approveBooking_ShouldDecrementSeats() {
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");
            pendingBooking.setNumberOfPeople(5);
            bookableTour.setAvailableSeats(30);

            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));
            when(tourRepository.save(any(Tour.class))).thenReturn(bookableTour);
            when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);

            bookingService.approveBooking(1L, dto);

            assertEquals(25, bookableTour.getAvailableSeats());
        }

        @Test
        @DisplayName("Should set tour to SOLD_OUT when last seats approved")
        void approveBooking_LastSeats_ShouldSetSoldOut() {
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");
            pendingBooking.setNumberOfPeople(5);
            bookableTour.setAvailableSeats(5);

            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));
            when(tourRepository.save(any(Tour.class))).thenReturn(bookableTour);
            when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);

            bookingService.approveBooking(1L, dto);

            assertEquals(0, bookableTour.getAvailableSeats());
            assertEquals(TourStatus.SOLD_OUT, bookableTour.getStatus());
        }
    }

    // ==================== REJECT BOOKING TESTS ====================

    @Nested
    @DisplayName("rejectBooking")
    class RejectBookingTests {

        @Test
        @DisplayName("Should reject booking successfully")
        void rejectBooking_Valid_ShouldSucceed() {
            BookingRejectionDto dto = new BookingRejectionDto();
            dto.setRejectionReason("Fully booked");

            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);

            BookingResponseDto result = bookingService.rejectBooking(1L, dto);

            assertEquals(BookingStatus.REJECTED, result.getStatus());
        }

        @Test
        @DisplayName("Should throw when not admin")
        void rejectBooking_NotAdmin_ShouldThrow() {
            BookingRejectionDto dto = new BookingRejectionDto();
            dto.setRejectionReason("Reason");

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(false);

            assertThrows(GlobalException.class, () -> bookingService.rejectBooking(1L, dto));
        }

        @Test
        @DisplayName("Should throw when booking not pending")
        void rejectBooking_NotPending_ShouldThrow() {
            BookingRejectionDto dto = new BookingRejectionDto();
            dto.setRejectionReason("Reason");
            pendingBooking.setStatus(BookingStatus.APPROVED);

            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));

            assertThrows(GlobalException.class, () -> bookingService.rejectBooking(1L, dto));
        }
    }

    // ==================== CANCEL BOOKING TESTS ====================

    @Nested
    @DisplayName("cancelBooking")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel own pending booking")
        void cancelBooking_OwnPending_ShouldSucceed() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);

            BookingResponseDto result = bookingService.cancelBooking(1L);

            assertEquals(BookingStatus.CANCELLED, result.getStatus());
        }

        @Test
        @DisplayName("Should throw when cancelling other user's booking")
        void cancelBooking_OtherUser_ShouldThrow() {
            when(jwtHelper.getCurrentUserId()).thenReturn(999L);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));

            assertThrows(GlobalException.class, () -> bookingService.cancelBooking(1L));
        }

        @Test
        @DisplayName("Should throw when booking not pending")
        void cancelBooking_NotPending_ShouldThrow() {
            pendingBooking.setStatus(BookingStatus.APPROVED);

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));

            assertThrows(GlobalException.class, () -> bookingService.cancelBooking(1L));
        }
    }

    // ==================== GET BOOKING TESTS ====================

    @Nested
    @DisplayName("getBookingById")
    class GetBookingByIdTests {

        @Test
        @DisplayName("Should return booking to owner")
        void getBookingById_Owner_ShouldSucceed() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(false);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));

            BookingResponseDto result = bookingService.getBookingById(1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return booking to admin")
        void getBookingById_Admin_ShouldSucceed() {
            when(jwtHelper.getCurrentUserId()).thenReturn(2L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));

            BookingResponseDto result = bookingService.getBookingById(1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw for non-owner non-admin")
        void getBookingById_NonOwnerNonAdmin_ShouldThrow() {
            when(jwtHelper.getCurrentUserId()).thenReturn(999L);
            when(jwtHelper.hasRole("ADMIN")).thenReturn(false);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(pendingBooking));

            assertThrows(GlobalException.class, () -> bookingService.getBookingById(1L));
        }
    }

    // ==================== GET BOOKINGS LIST TESTS ====================

    @Nested
    @DisplayName("getBookingsList")
    class GetBookingsListTests {

        @Test
        @DisplayName("Should return user's bookings")
        void getMyBookings_ShouldReturnUserBookings() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(bookingRepository.findByUserId(1L)).thenReturn(List.of(pendingBooking));

            List<BookingResponseDto> result = bookingService.getMyBookings();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return pending bookings for admin")
        void getPendingBookings_Admin_ShouldSucceed() {
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findByStatusOrderByCreatedAtAsc(BookingStatus.PENDING))
                    .thenReturn(List.of(pendingBooking));

            List<BookingResponseDto> result = bookingService.getPendingBookings();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw for non-admin getting pending bookings")
        void getPendingBookings_NonAdmin_ShouldThrow() {
            when(jwtHelper.hasRole("ADMIN")).thenReturn(false);

            assertThrows(GlobalException.class, () -> bookingService.getPendingBookings());
        }

        @Test
        @DisplayName("Should return bookings by tour for admin")
        void getBookingsByTour_Admin_ShouldSucceed() {
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findByTourId(1L)).thenReturn(List.of(pendingBooking));

            List<BookingResponseDto> result = bookingService.getBookingsByTour(1L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw for non-admin getting bookings by tour")
        void getBookingsByTour_NonAdmin_ShouldThrow() {
            when(jwtHelper.hasRole("ADMIN")).thenReturn(false);

            assertThrows(GlobalException.class, () -> bookingService.getBookingsByTour(1L));
        }

        @Test
        @DisplayName("Should return all bookings for admin")
        void getAllBookings_Admin_ShouldSucceed() {
            when(jwtHelper.hasRole("ADMIN")).thenReturn(true);
            when(bookingRepository.findAll()).thenReturn(List.of(pendingBooking));

            List<BookingResponseDto> result = bookingService.getAllBookings();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw for non-admin getting all bookings")
        void getAllBookings_NonAdmin_ShouldThrow() {
            when(jwtHelper.hasRole("ADMIN")).thenReturn(false);

            assertThrows(GlobalException.class, () -> bookingService.getAllBookings());
        }
    }

    // ==================== HELPER METHODS ====================

    private Tour createBookableTour() {
        Tour tour = new Tour();
        tour.setId(1L);
        tour.setName("Test Tour");
        tour.setPrice(new BigDecimal("1500"));
        tour.setDuration(7);
        tour.setMinParticipants(10);
        tour.setMaxParticipants(30);
        tour.setAvailableSeats(30);
        tour.setStartDate(LocalDateTime.now().plusDays(30));
        tour.setEndDate(LocalDateTime.now().plusDays(37));
        tour.setBookingDeadline(LocalDateTime.now().plusDays(25));
        tour.setCategory(TourCategory.CULTURAL);
        tour.setStatus(TourStatus.PUBLISHED);
        tour.setIsActive(true);
        tour.setDestinations(Arrays.asList("Paris", "London"));
        tour.setDepartureCity("Istanbul");
        return tour;
    }

    private UserEntity createUser(Long id, String email, UserRole role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setIsActive(true);
        return user;
    }

    private Booking createPendingBooking() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setTour(bookableTour);
        booking.setUserId(1L);
        booking.setUserEmail("user@test.com");
        booking.setUserName("Test User");
        booking.setUserPhone("5551234567");
        booking.setNumberOfPeople(2);
        booking.setStatus(BookingStatus.PENDING);
        return booking;
    }

    private BookingRequestDto createValidBookingDto() {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setTourId(1L);
        dto.setNumberOfPeople(2);
        dto.setUserName("John Doe");
        dto.setUserPhone("5551234567");
        return dto;
    }
}