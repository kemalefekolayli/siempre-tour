package siempretour.Tours;


import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Tours.Dto.TourCreateDto;
import com.siempretour.Tours.Dto.TourFilterDto;
import com.siempretour.Tours.Dto.TourResponseDto;
import com.siempretour.Tours.Dto.TourUpdateDto;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private JwtHelper jwtHelper;

    @InjectMocks
    private TourService tourService;

    private Tour sampleTour;
    private TourCreateDto createDto;
    private TourUpdateDto updateDto;

    @BeforeEach
    void setUp() {
        sampleTour = createSampleTour();
        createDto = createSampleCreateDto();
        updateDto = new TourUpdateDto();
    }

    // ==================== CREATE TOUR TESTS ====================

    @Nested
    @DisplayName("createTour")
    class CreateTourTests {

        @Test
        @DisplayName("Should create tour successfully")
        void createTour_ValidDto_ShouldSucceed() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
                Tour tour = invocation.getArgument(0);
                tour.setId(1L);
                return tour;
            });

            TourResponseDto result = tourService.createTour(createDto);

            assertNotNull(result);
            assertEquals(createDto.getName(), result.getName());
            assertEquals(createDto.getPrice(), result.getPrice());
            verify(tourRepository, times(1)).save(any(Tour.class));
        }

        @Test
        @DisplayName("Should throw exception when end date before start date")
        void createTour_EndDateBeforeStartDate_ShouldThrow() {
            createDto.setStartDate(LocalDateTime.now().plusDays(10));
            createDto.setEndDate(LocalDateTime.now().plusDays(5));

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);

            assertThrows(GlobalException.class, () -> tourService.createTour(createDto));
            verify(tourRepository, never()).save(any(Tour.class));
        }

        @Test
        @DisplayName("Should throw exception when discounted price >= regular price")
        void createTour_InvalidDiscountedPrice_ShouldThrow() {
            createDto.setPrice(new BigDecimal("1000"));
            createDto.setDiscountedPrice(new BigDecimal("1500"));

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);

            assertThrows(GlobalException.class, () -> tourService.createTour(createDto));
            verify(tourRepository, never()).save(any(Tour.class));
        }

        @Test
        @DisplayName("Should set available seats equal to max participants")
        void createTour_ShouldSetAvailableSeats() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
                Tour tour = invocation.getArgument(0);
                tour.setId(1L);
                return tour;
            });

            TourResponseDto result = tourService.createTour(createDto);

            assertEquals(createDto.getMaxParticipants(), result.getAvailableSeats());
        }

        @Test
        @DisplayName("Should accept discounted price less than regular price")
        void createTour_ValidDiscountedPrice_ShouldSucceed() {
            createDto.setPrice(new BigDecimal("1000"));
            createDto.setDiscountedPrice(new BigDecimal("800"));

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
                Tour tour = invocation.getArgument(0);
                tour.setId(1L);
                return tour;
            });

            TourResponseDto result = tourService.createTour(createDto);

            assertEquals(new BigDecimal("800"), result.getDiscountedPrice());
        }
    }

    // ==================== UPDATE TOUR TESTS ====================

    @Nested
    @DisplayName("updateTour")
    class UpdateTourTests {

        @Test
        @DisplayName("Should update tour successfully")
        void updateTour_ValidDto_ShouldSucceed() {
            updateDto.setName("Updated Name");

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.findById(1L)).thenReturn(Optional.of(sampleTour));
            when(tourRepository.save(any(Tour.class))).thenReturn(sampleTour);

            TourResponseDto result = tourService.updateTour(1L, updateDto);

            assertNotNull(result);
            verify(tourRepository, times(1)).save(any(Tour.class));
        }

        @Test
        @DisplayName("Should throw exception when tour not found")
        void updateTour_TourNotFound_ShouldThrow() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(GlobalException.class, () -> tourService.updateTour(999L, updateDto));
        }

        @Test
        @DisplayName("Should only update provided fields")
        void updateTour_PartialUpdate_ShouldOnlyUpdateProvided() {
            String originalName = sampleTour.getName();
            updateDto.setPrice(new BigDecimal("2000"));

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.findById(1L)).thenReturn(Optional.of(sampleTour));
            when(tourRepository.save(any(Tour.class))).thenReturn(sampleTour);

            tourService.updateTour(1L, updateDto);

            assertEquals(originalName, sampleTour.getName());
            assertEquals(new BigDecimal("2000"), sampleTour.getPrice());
        }

        @Test
        @DisplayName("Should adjust available seats when max participants decreased")
        void updateTour_DecreaseMaxParticipants_ShouldAdjustSeats() {
            sampleTour.setMaxParticipants(50);
            sampleTour.setAvailableSeats(50);
            updateDto.setMaxParticipants(30);

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.findById(1L)).thenReturn(Optional.of(sampleTour));
            when(tourRepository.save(any(Tour.class))).thenReturn(sampleTour);

            tourService.updateTour(1L, updateDto);

            assertEquals(30, sampleTour.getAvailableSeats());
        }

        @Test
        @DisplayName("Should not adjust seats when max participants increased")
        void updateTour_IncreaseMaxParticipants_ShouldNotAdjustSeats() {
            sampleTour.setMaxParticipants(30);
            sampleTour.setAvailableSeats(20);
            updateDto.setMaxParticipants(50);

            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.findById(1L)).thenReturn(Optional.of(sampleTour));
            when(tourRepository.save(any(Tour.class))).thenReturn(sampleTour);

            tourService.updateTour(1L, updateDto);

            assertEquals(20, sampleTour.getAvailableSeats());
        }
    }

    // ==================== DELETE TOUR TESTS ====================

    @Nested
    @DisplayName("deleteTour")
    class DeleteTourTests {

        @Test
        @DisplayName("Should soft delete tour")
        void deleteTour_ShouldSoftDelete() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.findById(1L)).thenReturn(Optional.of(sampleTour));
            when(tourRepository.save(any(Tour.class))).thenReturn(sampleTour);

            tourService.deleteTour(1L);

            assertFalse(sampleTour.getIsActive());
            assertEquals(TourStatus.CANCELLED, sampleTour.getStatus());
            verify(tourRepository, times(1)).save(sampleTour);
        }

        @Test
        @DisplayName("Should throw exception when tour not found")
        void deleteTour_TourNotFound_ShouldThrow() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(GlobalException.class, () -> tourService.deleteTour(999L));
        }
    }

    // ==================== GET TOUR TESTS ====================

    @Nested
    @DisplayName("getTourById")
    class GetTourByIdTests {

        @Test
        @DisplayName("Should return tour when found")
        void getTourById_Found_ShouldReturnTour() {
            when(tourRepository.findById(1L)).thenReturn(Optional.of(sampleTour));

            TourResponseDto result = tourService.getTourById(1L);

            assertNotNull(result);
            assertEquals(sampleTour.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw exception when not found")
        void getTourById_NotFound_ShouldThrow() {
            when(tourRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(GlobalException.class, () -> tourService.getTourById(999L));
        }
    }

    // ==================== FILTER TOUR TESTS ====================

    @Nested
    @DisplayName("filterTours")
    class FilterTourTests {

        @Test
        @DisplayName("Should filter tours with pagination")
        void filterTours_WithPagination_ShouldReturnPagedResults() {
            TourFilterDto filter = new TourFilterDto();
            filter.setPage(0);
            filter.setSize(10);

            Page<Tour> tourPage = new PageImpl<>(List.of(sampleTour));
            when(tourRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(tourPage);

            var result = tourService.filterTours(filter);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(0, result.getPage());
        }

        @Test
        @DisplayName("Should use default page size when not provided")
        void filterTours_NoPageSize_ShouldUseDefault() {
            TourFilterDto filter = new TourFilterDto();

            Page<Tour> tourPage = new PageImpl<>(List.of(sampleTour));
            when(tourRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(tourPage);

            var result = tourService.filterTours(filter);

            assertEquals(20, result.getSize());
        }

        @Test
        @DisplayName("Should limit page size to 100")
        void filterTours_LargePageSize_ShouldLimitTo100() {
            TourFilterDto filter = new TourFilterDto();
            filter.setSize(200);

            Page<Tour> tourPage = new PageImpl<>(List.of(sampleTour));
            when(tourRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(tourPage);

            var result = tourService.filterTours(filter);

            assertEquals(100, result.getSize());
        }

        @Test
        @DisplayName("Should use default sort when invalid sortBy provided")
        void filterTours_InvalidSortBy_ShouldUseDefault() {
            TourFilterDto filter = new TourFilterDto();
            filter.setSortBy("invalidField");

            Page<Tour> tourPage = new PageImpl<>(List.of(sampleTour));
            when(tourRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(tourPage);

            assertDoesNotThrow(() -> tourService.filterTours(filter));
        }
    }

    // ==================== GET TOURS LIST TESTS ====================

    @Nested
    @DisplayName("getTours List Methods")
    class GetToursListTests {

        @Test
        @DisplayName("Should return all tours")
        void getAllTours_ShouldReturnAllTours() {
            when(tourRepository.findAll()).thenReturn(List.of(sampleTour));

            List<TourResponseDto> result = tourService.getAllTours();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return active tours only")
        void getActiveTours_ShouldReturnActiveOnly() {
            when(tourRepository.findByIsActiveTrue()).thenReturn(List.of(sampleTour));

            List<TourResponseDto> result = tourService.getActiveTours();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return published tours only")
        void getPublishedTours_ShouldReturnPublishedOnly() {
            when(tourRepository.findByIsActiveTrueAndStatusAndStartDateAfter(
                    eq(TourStatus.PUBLISHED), any(LocalDateTime.class)))
                    .thenReturn(List.of(sampleTour));

            List<TourResponseDto> result = tourService.getPublishedTours();

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return tours created by current user")
        void getMyTours_ShouldReturnUserTours() {
            when(jwtHelper.getCurrentUserId()).thenReturn(1L);
            when(tourRepository.findByCreatedBy(1L)).thenReturn(List.of(sampleTour));

            List<TourResponseDto> result = tourService.getMyTours();

            assertEquals(1, result.size());
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
        tour.setStatus(TourStatus.DRAFT);
        tour.setIsActive(true);
        tour.setDestinations(Arrays.asList("Paris", "London"));
        tour.setDepartureCity("Istanbul");
        tour.setCreatedBy(1L);
        return tour;
    }

    private TourCreateDto createSampleCreateDto() {
        TourCreateDto dto = new TourCreateDto();
        dto.setName("New Tour");
        dto.setPrice(new BigDecimal("2000"));
        dto.setDestinations(Arrays.asList("Rome", "Venice"));
        dto.setDepartureCity("Istanbul");
        dto.setDuration(5);
        dto.setMinParticipants(5);
        dto.setMaxParticipants(20);
        dto.setStartDate(LocalDateTime.now().plusDays(30));
        dto.setEndDate(LocalDateTime.now().plusDays(35));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(25));
        dto.setCategory(TourCategory.HISTORICAL);
        return dto;
    }
}