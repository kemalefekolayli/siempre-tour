package com.siempretour.Tours;

import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TourModelTest {

    private Tour tour;

    @BeforeEach
    void setUp() {
        tour = createSampleTour();
    }

    // ==================== isBookable TESTS ====================

    @Nested
    @DisplayName("isBookable")
    class IsBookableTests {

        @Test
        @DisplayName("Should be bookable when all conditions met")
        void isBookable_AllConditionsMet_ShouldReturnTrue() {
            assertTrue(tour.isBookable());
        }

        @Test
        @DisplayName("Should not be bookable when inactive")
        void isBookable_Inactive_ShouldReturnFalse() {
            tour.setIsActive(false);
            assertFalse(tour.isBookable());
        }

        @Test
        @DisplayName("Should not be bookable when status is DRAFT")
        void isBookable_DraftStatus_ShouldReturnFalse() {
            tour.setStatus(TourStatus.DRAFT);
            assertFalse(tour.isBookable());
        }

        @Test
        @DisplayName("Should not be bookable when status is SOLD_OUT")
        void isBookable_SoldOutStatus_ShouldReturnFalse() {
            tour.setStatus(TourStatus.SOLD_OUT);
            assertFalse(tour.isBookable());
        }

        @Test
        @DisplayName("Should not be bookable when status is CANCELLED")
        void isBookable_CancelledStatus_ShouldReturnFalse() {
            tour.setStatus(TourStatus.CANCELLED);
            assertFalse(tour.isBookable());
        }

        @Test
        @DisplayName("Should not be bookable when status is COMPLETED")
        void isBookable_CompletedStatus_ShouldReturnFalse() {
            tour.setStatus(TourStatus.COMPLETED);
            assertFalse(tour.isBookable());
        }

        @Test
        @DisplayName("Should not be bookable when no available seats")
        void isBookable_NoSeats_ShouldReturnFalse() {
            tour.setAvailableSeats(0);
            assertFalse(tour.isBookable());
        }

        @Test
        @DisplayName("Should not be bookable after booking deadline")
        void isBookable_AfterDeadline_ShouldReturnFalse() {
            tour.setBookingDeadline(LocalDateTime.now().minusDays(1));
            assertFalse(tour.isBookable());
        }

        @Test
        @DisplayName("Should use startDate when bookingDeadline is null")
        void isBookable_NullDeadline_ShouldUseStartDate() {
            tour.setBookingDeadline(null);
            tour.setStartDate(LocalDateTime.now().plusDays(10));
            assertTrue(tour.isBookable());
        }

        @Test
        @DisplayName("Should not be bookable after startDate when deadline is null")
        void isBookable_NullDeadlineAfterStart_ShouldReturnFalse() {
            tour.setBookingDeadline(null);
            tour.setStartDate(LocalDateTime.now().minusDays(1));
            assertFalse(tour.isBookable());
        }
    }

    // ==================== decrementAvailableSeats TESTS ====================

    @Nested
    @DisplayName("decrementAvailableSeats")
    class DecrementAvailableSeatsTests {

        @Test
        @DisplayName("Should decrement seats correctly")
        void decrementSeats_ValidCount_ShouldDecrement() {
            tour.setAvailableSeats(30);
            tour.decrementAvailableSeats(5);
            assertEquals(25, tour.getAvailableSeats());
        }

        @Test
        @DisplayName("Should set status to SOLD_OUT when seats become zero")
        void decrementSeats_BecomesZero_ShouldSetSoldOut() {
            tour.setAvailableSeats(5);
            tour.decrementAvailableSeats(5);
            assertEquals(0, tour.getAvailableSeats());
            assertEquals(TourStatus.SOLD_OUT, tour.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when not enough seats")
        void decrementSeats_NotEnough_ShouldThrow() {
            tour.setAvailableSeats(5);
            assertThrows(IllegalStateException.class, () -> tour.decrementAvailableSeats(10));
        }

        @Test
        @DisplayName("Should handle decrement by 1")
        void decrementSeats_ByOne_ShouldWork() {
            tour.setAvailableSeats(10);
            tour.decrementAvailableSeats(1);
            assertEquals(9, tour.getAvailableSeats());
        }

        @Test
        @DisplayName("Should throw for zero seats decrement")
        void decrementSeats_ByZero_ShouldWork() {
            tour.setAvailableSeats(10);
            tour.decrementAvailableSeats(0);
            assertEquals(10, tour.getAvailableSeats());
        }
    }

    // ==================== LIFECYCLE CALLBACK TESTS ====================

    @Nested
    @DisplayName("Lifecycle Callbacks")
    class LifecycleCallbackTests {

        @Test
        @DisplayName("onCreate should set timestamps")
        void onCreate_ShouldSetTimestamps() {
            Tour newTour = new Tour();
            newTour.setName("Test");
            newTour.setMaxParticipants(10);

            newTour.onCreate();

            assertNotNull(newTour.getCreatedAt());
            assertNotNull(newTour.getUpdatedAt());
            assertEquals(newTour.getCreatedAt(), newTour.getUpdatedAt());
        }

        @Test
        @DisplayName("onCreate should set availableSeats from maxParticipants")
        void onCreate_ShouldSetAvailableSeats() {
            Tour newTour = new Tour();
            newTour.setMaxParticipants(25);

            newTour.onCreate();

            assertEquals(25, newTour.getAvailableSeats());
        }

        @Test
        @DisplayName("onCreate should not override existing availableSeats")
        void onCreate_ExistingSeats_ShouldNotOverride() {
            Tour newTour = new Tour();
            newTour.setMaxParticipants(25);
            newTour.setAvailableSeats(10);

            newTour.onCreate();

            assertEquals(10, newTour.getAvailableSeats());
        }

        @Test
        @DisplayName("onUpdate should update updatedAt timestamp")
        void onUpdate_ShouldUpdateTimestamp() throws InterruptedException {
            tour.onCreate();
            LocalDateTime originalUpdatedAt = tour.getUpdatedAt();

            Thread.sleep(10); // Small delay to ensure different timestamp
            tour.onUpdate();

            assertTrue(tour.getUpdatedAt().isAfter(originalUpdatedAt));
        }
    }

    // ==================== HELPER METHODS ====================

    private Tour createSampleTour() {
        Tour tour = new Tour();
        tour.setId(1L);
        tour.setName("Sample Tour");
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
        return tour;
    }
}
