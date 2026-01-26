package com.siempretour.Booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.siempretour.Booking.Dto.BookingApprovalDto;
import com.siempretour.Booking.Dto.BookingRejectionDto;
import com.siempretour.Booking.Dto.BookingRequestDto;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import com.siempretour.Tours.TourRepository;
import com.siempretour.Security.JwtTokenProvider;
import com.siempretour.User.UserEntity;
import com.siempretour.User.UserEntityRepository;
import com.siempretour.User.UserRole;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TourRepository tourRepository;
    @Autowired
    private UserEntityRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private ObjectMapper objectMapper;
    private String adminToken;
    private String userToken;
    private String user2Token;
    private UserEntity adminUser;
    private UserEntity regularUser;
    private UserEntity regularUser2;
    private Tour bookableTour;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create admin user
        adminUser = new UserEntity();
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("Admin@123"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser.setEmailVerified(true);
        adminUser = userRepository.save(adminUser);

        // Create regular user
        regularUser = new UserEntity();
        regularUser.setEmail("user@test.com");
        regularUser.setPassword(passwordEncoder.encode("User@123"));
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setRole(UserRole.USER);
        regularUser.setIsActive(true);
        regularUser.setEmailVerified(true);
        regularUser = userRepository.save(regularUser);

        // Create second regular user
        regularUser2 = new UserEntity();
        regularUser2.setEmail("user2@test.com");
        regularUser2.setPassword(passwordEncoder.encode("User2@123"));
        regularUser2.setFirstName("Regular2");
        regularUser2.setLastName("User2");
        regularUser2.setRole(UserRole.USER);
        regularUser2.setIsActive(true);
        regularUser2.setEmailVerified(true);
        regularUser2 = userRepository.save(regularUser2);

        // Generate tokens
        adminToken = jwtTokenProvider.createToken(adminUser.getEmail(), adminUser.getId(), UserRole.ADMIN.name());
        userToken = jwtTokenProvider.createToken(regularUser.getEmail(), regularUser.getId(), UserRole.USER.name());
        user2Token = jwtTokenProvider.createToken(regularUser2.getEmail(), regularUser2.getId(), UserRole.USER.name());

        // Create a bookable tour
        bookableTour = createBookableTour();
    }

    // ==================== BOOKING CREATION TESTS ====================

    @Nested
    @DisplayName("POST /api/bookings - Create Booking")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully with valid data")
        void createBooking_WithValidData_ShouldSucceed() throws Exception {
            BookingRequestDto dto = createValidBookingDto();

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.tourId").value(bookableTour.getId()))
                    .andExpect(jsonPath("$.userId").value(regularUser.getId()))
                    .andExpect(jsonPath("$.numberOfPeople").value(dto.getNumberOfPeople()))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.userName").value(dto.getUserName()))
                    .andExpect(jsonPath("$.userPhone").value(dto.getUserPhone()));
        }

        @Test
        @DisplayName("Should reject booking without authentication")
        void createBooking_WithoutAuth_ShouldBeUnauthorized() throws Exception {
            BookingRequestDto dto = createValidBookingDto();

            mockMvc.perform(post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject booking for non-existent tour")
        void createBooking_NonExistentTour_ShouldFail() throws Exception {
            BookingRequestDto dto = createValidBookingDto();
            dto.setTourId(999999L);

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject booking for non-bookable tour (draft status)")
        void createBooking_DraftTour_ShouldFail() throws Exception {
            Tour draftTour = createBookableTour();
            draftTour.setStatus(TourStatus.DRAFT);
            tourRepository.save(draftTour);

            BookingRequestDto dto = createValidBookingDto();
            dto.setTourId(draftTour.getId());

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking for inactive tour")
        void createBooking_InactiveTour_ShouldFail() throws Exception {
            Tour inactiveTour = createBookableTour();
            inactiveTour.setIsActive(false);
            tourRepository.save(inactiveTour);

            BookingRequestDto dto = createValidBookingDto();
            dto.setTourId(inactiveTour.getId());

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking when not enough seats available")
        void createBooking_NotEnoughSeats_ShouldFail() throws Exception {
            bookableTour.setAvailableSeats(2);
            tourRepository.save(bookableTour);

            BookingRequestDto dto = createValidBookingDto();
            dto.setNumberOfPeople(5);

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with zero people")
        void createBooking_ZeroPeople_ShouldFail() throws Exception {
            BookingRequestDto dto = createValidBookingDto();
            dto.setNumberOfPeople(0);

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with negative people count")
        void createBooking_NegativePeople_ShouldFail() throws Exception {
            BookingRequestDto dto = createValidBookingDto();
            dto.setNumberOfPeople(-1);

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with missing required fields")
        void createBooking_MissingFields_ShouldFail() throws Exception {
            BookingRequestDto dto = new BookingRequestDto();
            // Missing all required fields

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with empty user name")
        void createBooking_EmptyUserName_ShouldFail() throws Exception {
            BookingRequestDto dto = createValidBookingDto();
            dto.setUserName("");

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with empty phone")
        void createBooking_EmptyPhone_ShouldFail() throws Exception {
            BookingRequestDto dto = createValidBookingDto();
            dto.setUserPhone("");

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should create booking with optional user message")
        void createBooking_WithUserMessage_ShouldSucceed() throws Exception {
            BookingRequestDto dto = createValidBookingDto();
            dto.setUserMessage("I need a vegetarian meal option");

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should reject booking for sold out tour")
        void createBooking_SoldOutTour_ShouldFail() throws Exception {
            bookableTour.setStatus(TourStatus.SOLD_OUT);
            tourRepository.save(bookableTour);

            BookingRequestDto dto = createValidBookingDto();

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking after booking deadline")
        void createBooking_AfterDeadline_ShouldFail() throws Exception {
            bookableTour.setBookingDeadline(LocalDateTime.now().minusDays(1));
            tourRepository.save(bookableTour);

            BookingRequestDto dto = createValidBookingDto();

            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== BOOKING APPROVAL TESTS ====================

    @Nested
    @DisplayName("POST /api/bookings/{id}/approve - Approve Booking")
    class ApproveBookingTests {

        @Test
        @DisplayName("Should approve booking successfully as admin")
        void approveBooking_AsAdmin_ShouldSucceed() throws Exception {
            Booking booking = createPendingBooking();
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved for group booking");

            int initialSeats = bookableTour.getAvailableSeats();

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/approve")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.approvedBy").value(adminUser.getId()));

            // Verify seats were decremented
            Tour updatedTour = tourRepository.findById(bookableTour.getId()).orElseThrow();
            assertEquals(initialSeats - booking.getNumberOfPeople(), updatedTour.getAvailableSeats());
        }

        @Test
        @DisplayName("Should reject approval by non-admin user")
        void approveBooking_AsNonAdmin_ShouldBeForbidden() throws Exception {
            Booking booking = createPendingBooking();
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/approve")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden()); // Returns 403 for non-admin
        }

        @Test
        @DisplayName("Should reject approval of already approved booking")
        void approveBooking_AlreadyApproved_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();
            booking.setStatus(BookingStatus.APPROVED);
            bookingRepository.save(booking);

            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved again");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/approve")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject approval of rejected booking")
        void approveBooking_AlreadyRejected_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();
            booking.setStatus(BookingStatus.REJECTED);
            bookingRepository.save(booking);

            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/approve")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject approval when not enough seats left")
        void approveBooking_NotEnoughSeats_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();
            booking.setNumberOfPeople(10);
            bookingRepository.save(booking);

            bookableTour.setAvailableSeats(5);
            tourRepository.save(bookableTour);

            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/approve")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 for non-existent booking")
        void approveBooking_NonExistent_ShouldReturn404() throws Exception {
            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");

            mockMvc.perform(post("/api/bookings/999999/approve")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict()); // RESERVATION_COULD_NOT_BE_CREATED
        }

        @Test
        @DisplayName("Should set tour to SOLD_OUT when last seats approved")
        void approveBooking_LastSeats_ShouldSetSoldOut() throws Exception {
            bookableTour.setAvailableSeats(3);
            tourRepository.save(bookableTour);

            Booking booking = createPendingBooking();
            booking.setNumberOfPeople(3);
            bookingRepository.save(booking);

            BookingApprovalDto dto = new BookingApprovalDto();
            dto.setAdminNote("Approved");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/approve")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            Tour updatedTour = tourRepository.findById(bookableTour.getId()).orElseThrow();
            assertEquals(0, updatedTour.getAvailableSeats());
            assertEquals(TourStatus.SOLD_OUT, updatedTour.getStatus());
        }
    }

    // ==================== BOOKING REJECTION TESTS ====================

    @Nested
    @DisplayName("POST /api/bookings/{id}/reject - Reject Booking")
    class RejectBookingTests {

        @Test
        @DisplayName("Should reject booking successfully as admin")
        void rejectBooking_AsAdmin_ShouldSucceed() throws Exception {
            Booking booking = createPendingBooking();
            BookingRejectionDto dto = new BookingRejectionDto();
            dto.setRejectionReason("Tour is fully booked with priority guests");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/reject")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectedBy").value(adminUser.getId()));
        }

        @Test
        @DisplayName("Should reject rejection by non-admin user")
        void rejectBooking_AsNonAdmin_ShouldBeForbidden() throws Exception {
            Booking booking = createPendingBooking();
            BookingRejectionDto dto = new BookingRejectionDto();
            dto.setRejectionReason("Rejected");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/reject")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject rejection of already approved booking")
        void rejectBooking_AlreadyApproved_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();
            booking.setStatus(BookingStatus.APPROVED);
            bookingRepository.save(booking);

            BookingRejectionDto dto = new BookingRejectionDto();
            dto.setRejectionReason("Rejected");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/reject")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should require rejection reason")
        void rejectBooking_MissingReason_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();
            BookingRejectionDto dto = new BookingRejectionDto();
            // Missing rejection reason

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/reject")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject with empty rejection reason")
        void rejectBooking_EmptyReason_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();
            BookingRejectionDto dto = new BookingRejectionDto();
            dto.setRejectionReason("");

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/reject")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== BOOKING CANCELLATION TESTS ====================

    @Nested
    @DisplayName("POST /api/bookings/{id}/cancel - Cancel Booking")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel own pending booking successfully")
        void cancelBooking_OwnPending_ShouldSucceed() throws Exception {
            Booking booking = createPendingBooking();

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/cancel")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should reject cancellation of another user's booking")
        void cancelBooking_OtherUserBooking_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/cancel")
                    .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject cancellation of approved booking")
        void cancelBooking_AlreadyApproved_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();
            booking.setStatus(BookingStatus.APPROVED);
            bookingRepository.save(booking);

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/cancel")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject cancellation of already cancelled booking")
        void cancelBooking_AlreadyCancelled_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            mockMvc.perform(post("/api/bookings/" + booking.getId() + "/cancel")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== BOOKING RETRIEVAL TESTS ====================

    @Nested
    @DisplayName("GET /api/bookings - Retrieve Bookings")
    class GetBookingTests {

        @Test
        @DisplayName("Should get own booking by ID")
        void getBookingById_OwnBooking_ShouldSucceed() throws Exception {
            Booking booking = createPendingBooking();

            mockMvc.perform(get("/api/bookings/" + booking.getId())
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(booking.getId()));
        }

        @Test
        @DisplayName("Should allow admin to get any booking by ID")
        void getBookingById_AsAdmin_ShouldSucceed() throws Exception {
            Booking booking = createPendingBooking();

            mockMvc.perform(get("/api/bookings/" + booking.getId())
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(booking.getId()));
        }

        @Test
        @DisplayName("Should reject access to other user's booking")
        void getBookingById_OtherUserBooking_ShouldFail() throws Exception {
            Booking booking = createPendingBooking();

            mockMvc.perform(get("/api/bookings/" + booking.getId())
                    .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should get user's own bookings")
        void getMyBookings_ShouldReturnUserBookings() throws Exception {
            createPendingBooking();
            createPendingBooking();

            mockMvc.perform(get("/api/bookings/me")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].userId", everyItem(equalTo(regularUser.getId().intValue()))));
        }

        @Test
        @DisplayName("Should return empty list when user has no bookings")
        void getMyBookings_NoBookings_ShouldReturnEmpty() throws Exception {
            mockMvc.perform(get("/api/bookings/me")
                    .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should get pending bookings as admin")
        void getPendingBookings_AsAdmin_ShouldSucceed() throws Exception {
            createPendingBooking();
            Booking approved = createPendingBooking();
            approved.setStatus(BookingStatus.APPROVED);
            bookingRepository.save(approved);

            mockMvc.perform(get("/api/bookings/pending")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].status", everyItem(equalTo("PENDING"))));
        }

        @Test
        @DisplayName("Should reject pending bookings access by non-admin")
        void getPendingBookings_AsNonAdmin_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/bookings/pending")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should get bookings by tour as admin")
        void getBookingsByTour_AsAdmin_ShouldSucceed() throws Exception {
            createPendingBooking();

            mockMvc.perform(get("/api/bookings/tour/" + bookableTour.getId())
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].tourId", everyItem(equalTo(bookableTour.getId().intValue()))));
        }

        @Test
        @DisplayName("Should reject bookings by tour access by non-admin")
        void getBookingsByTour_AsNonAdmin_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/bookings/tour/" + bookableTour.getId())
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should get all bookings as admin")
        void getAllBookings_AsAdmin_ShouldSucceed() throws Exception {
            createPendingBooking();

            mockMvc.perform(get("/api/bookings")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should reject all bookings access by non-admin")
        void getAllBookings_AsNonAdmin_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/bookings")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== HELPER METHODS ====================

    private BookingRequestDto createValidBookingDto() {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setTourId(bookableTour.getId());
        dto.setNumberOfPeople(2);
        dto.setUserName("John Doe");
        dto.setUserPhone("5551234567");
        return dto;
    }

    private Tour createBookableTour() {
        Tour tour = new Tour();
        tour.setName("Bookable Tour " + System.currentTimeMillis());
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
        tour.setDestinations(new ArrayList<>(Arrays.asList("Paris", "London")));
        tour.setDepartureCity("Istanbul");
        tour.setCreatedBy(adminUser.getId());
        return tourRepository.save(tour);
    }

    private Booking createPendingBooking() {
        Booking booking = new Booking();
        booking.setTour(bookableTour);
        booking.setUserId(regularUser.getId());
        booking.setUserEmail(regularUser.getEmail());
        booking.setUserName("Test User");
        booking.setUserPhone("5551234567");
        booking.setNumberOfPeople(2);
        booking.setStatus(BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }
}