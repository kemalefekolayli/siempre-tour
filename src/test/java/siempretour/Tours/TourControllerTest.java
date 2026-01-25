package siempretour.Tours;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.siempretour.Tours.Dto.TourCreateDto;
import com.siempretour.Tours.Dto.TourFilterDto;
import com.siempretour.Tours.Dto.TourUpdateDto;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import com.siempretour.Security.JwtTokenProvider;
import com.siempretour.Tours.TourRepository;
import com.siempretour.User.UserEntity;
import com.siempretour.User.UserEntityRepository;
import com.siempretour.User.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TourControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    private UserEntity adminUser;
    private UserEntity regularUser;

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

        // Generate tokens
        adminToken = jwtTokenProvider.createToken(adminUser.getEmail(), adminUser.getId(), UserRole.ADMIN.name());
        userToken = jwtTokenProvider.createToken(regularUser.getEmail(), regularUser.getId(), UserRole.USER.name());
    }

    // ==================== TOUR CREATION TESTS ====================

    @Nested
    @DisplayName("POST /api/tours - Create Tour")
    class CreateTourTests {

        @Test
        @DisplayName("Should create tour successfully with valid data as admin")
        void createTour_WithValidData_ShouldSucceed() throws Exception {
            TourCreateDto dto = createValidTourDto();

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value(dto.getName()))
                    .andExpect(jsonPath("$.price").value(dto.getPrice().doubleValue()))
                    .andExpect(jsonPath("$.duration").value(dto.getDuration()))
                    .andExpect(jsonPath("$.category").value(dto.getCategory().name()));
        }

        @Test
        @DisplayName("Should reject tour creation by non-admin user")
        void createTour_AsNonAdmin_ShouldBeForbidden() throws Exception {
            TourCreateDto dto = createValidTourDto();

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject tour creation without authentication")
        void createTour_WithoutAuth_ShouldBeUnauthorized() throws Exception {
            TourCreateDto dto = createValidTourDto();

            mockMvc.perform(post("/api/tours")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject tour with missing required fields")
        void createTour_WithMissingFields_ShouldFail() throws Exception {
            TourCreateDto dto = new TourCreateDto();
            // Missing all required fields

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tour with end date before start date")
        void createTour_WithInvalidDateRange_ShouldFail() throws Exception {
            TourCreateDto dto = createValidTourDto();
            dto.setStartDate(LocalDateTime.now().plusDays(10));
            dto.setEndDate(LocalDateTime.now().plusDays(5)); // End before start

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tour with discounted price >= regular price")
        void createTour_WithInvalidDiscountedPrice_ShouldFail() throws Exception {
            TourCreateDto dto = createValidTourDto();
            dto.setPrice(new BigDecimal("1000"));
            dto.setDiscountedPrice(new BigDecimal("1500")); // Higher than regular

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tour with zero price")
        void createTour_WithZeroPrice_ShouldFail() throws Exception {
            TourCreateDto dto = createValidTourDto();
            dto.setPrice(BigDecimal.ZERO);

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tour with negative price")
        void createTour_WithNegativePrice_ShouldFail() throws Exception {
            TourCreateDto dto = createValidTourDto();
            dto.setPrice(new BigDecimal("-100"));

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tour with zero duration")
        void createTour_WithZeroDuration_ShouldFail() throws Exception {
            TourCreateDto dto = createValidTourDto();
            dto.setDuration(0);

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tour with empty destinations list")
        void createTour_WithEmptyDestinations_ShouldFail() throws Exception {
            TourCreateDto dto = createValidTourDto();
            dto.setDestinations(List.of());

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should create cruise tour with ship details")
        void createTour_CruiseWithShipDetails_ShouldSucceed() throws Exception {
            TourCreateDto dto = createValidTourDto();
            dto.setCategory(TourCategory.CRUISE);
            dto.setShipName("MSC Fantasia");
            dto.setShipCompany("MSC Cruises");

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.shipName").value("MSC Fantasia"))
                    .andExpect(jsonPath("$.shipCompany").value("MSC Cruises"));
        }

        @ParameterizedTest
        @EnumSource(TourCategory.class)
        @DisplayName("Should create tour with any valid category")
        void createTour_WithAllCategories_ShouldSucceed(TourCategory category) throws Exception {
            TourCreateDto dto = createValidTourDto();
            dto.setCategory(category);

            mockMvc.perform(post("/api/tours")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.category").value(category.name()));
        }
    }

    // ==================== TOUR UPDATE TESTS ====================

    @Nested
    @DisplayName("PUT /api/tours/{id} - Update Tour")
    class UpdateTourTests {

        @Test
        @DisplayName("Should update tour successfully as admin")
        void updateTour_WithValidData_ShouldSucceed() throws Exception {
            Tour tour = createAndSaveTour();
            TourUpdateDto dto = new TourUpdateDto();
            dto.setName("Updated Tour Name");
            dto.setPrice(new BigDecimal("2500"));

            mockMvc.perform(put("/api/tours/" + tour.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Tour Name"))
                    .andExpect(jsonPath("$.price").value(2500));
        }

        @Test
        @DisplayName("Should reject update by non-admin user")
        void updateTour_AsNonAdmin_ShouldBeForbidden() throws Exception {
            Tour tour = createAndSaveTour();
            TourUpdateDto dto = new TourUpdateDto();
            dto.setName("Updated Tour Name");

            mockMvc.perform(put("/api/tours/" + tour.getId())
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 for non-existent tour")
        void updateTour_NonExistent_ShouldReturn404() throws Exception {
            TourUpdateDto dto = new TourUpdateDto();
            dto.setName("Updated Tour Name");

            mockMvc.perform(put("/api/tours/999999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should update tour status")
        void updateTour_ChangeStatus_ShouldSucceed() throws Exception {
            Tour tour = createAndSaveTour();
            TourUpdateDto dto = new TourUpdateDto();
            dto.setStatus(TourStatus.PUBLISHED);

            mockMvc.perform(put("/api/tours/" + tour.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("Should adjust available seats when max participants decreased")
        void updateTour_DecreaseMaxParticipants_ShouldAdjustSeats() throws Exception {
            Tour tour = createAndSaveTour();
            tour.setMaxParticipants(50);
            tour.setAvailableSeats(50);
            tourRepository.save(tour);

            TourUpdateDto dto = new TourUpdateDto();
            dto.setMaxParticipants(30);

            mockMvc.perform(put("/api/tours/" + tour.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.maxParticipants").value(30))
                    .andExpect(jsonPath("$.availableSeats").value(30));
        }

        @Test
        @DisplayName("Should only update provided fields (partial update)")
        void updateTour_PartialUpdate_ShouldOnlyUpdateProvidedFields() throws Exception {
            Tour tour = createAndSaveTour();
            String originalName = tour.getName();
            BigDecimal originalPrice = tour.getPrice();

            TourUpdateDto dto = new TourUpdateDto();
            dto.setDuration(14); // Only update duration

            mockMvc.perform(put("/api/tours/" + tour.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.duration").value(14))
                    .andExpect(jsonPath("$.name").value(originalName))
                    .andExpect(jsonPath("$.price").value(originalPrice.doubleValue()));
        }
    }

    // ==================== TOUR DELETION TESTS ====================

    @Nested
    @DisplayName("DELETE /api/tours/{id} - Delete Tour")
    class DeleteTourTests {

        @Test
        @DisplayName("Should soft delete tour as admin")
        void deleteTour_AsAdmin_ShouldSoftDelete() throws Exception {
            Tour tour = createAndSaveTour();

            mockMvc.perform(delete("/api/tours/" + tour.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // Verify soft delete
            Tour deletedTour = tourRepository.findById(tour.getId()).orElseThrow();
            assertFalse(deletedTour.getIsActive());
            assertEquals(TourStatus.CANCELLED, deletedTour.getStatus());
        }

        @Test
        @DisplayName("Should reject deletion by non-admin user")
        void deleteTour_AsNonAdmin_ShouldBeForbidden() throws Exception {
            Tour tour = createAndSaveTour();

            mockMvc.perform(delete("/api/tours/" + tour.getId())
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 for non-existent tour")
        void deleteTour_NonExistent_ShouldReturn404() throws Exception {
            mockMvc.perform(delete("/api/tours/999999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== TOUR RETRIEVAL TESTS ====================

    @Nested
    @DisplayName("GET /api/tours - Retrieve Tours")
    class GetTourTests {

        @Test
        @DisplayName("Should get tour by ID (public endpoint)")
        void getTourById_ShouldSucceed() throws Exception {
            Tour tour = createAndSaveTour();

            mockMvc.perform(get("/api/tours/" + tour.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(tour.getId()))
                    .andExpect(jsonPath("$.name").value(tour.getName()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent tour")
        void getTourById_NonExistent_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/tours/999999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should get published tours without authentication")
        void getPublishedTours_WithoutAuth_ShouldSucceed() throws Exception {
            // Create published tour
            Tour publishedTour = createAndSaveTour();
            publishedTour.setStatus(TourStatus.PUBLISHED);
            publishedTour.setIsActive(true);
            publishedTour.setStartDate(LocalDateTime.now().plusDays(10));
            tourRepository.save(publishedTour);

            mockMvc.perform(get("/api/tours/published"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("Should get active tours without authentication")
        void getActiveTours_WithoutAuth_ShouldSucceed() throws Exception {
            Tour activeTour = createAndSaveTour();
            activeTour.setIsActive(true);
            tourRepository.save(activeTour);

            mockMvc.perform(get("/api/tours/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("Should not include inactive tours in published list")
        void getPublishedTours_ShouldExcludeInactive() throws Exception {
            Tour inactiveTour = createAndSaveTour();
            inactiveTour.setStatus(TourStatus.PUBLISHED);
            inactiveTour.setIsActive(false);
            tourRepository.save(inactiveTour);

            mockMvc.perform(get("/api/tours/published"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + inactiveTour.getId() + ")]").doesNotExist());
        }

        @Test
        @DisplayName("Should not include past tours in published list")
        void getPublishedTours_ShouldExcludePastTours() throws Exception {
            Tour pastTour = createAndSaveTour();
            pastTour.setStatus(TourStatus.PUBLISHED);
            pastTour.setIsActive(true);
            pastTour.setStartDate(LocalDateTime.now().minusDays(10));
            tourRepository.save(pastTour);

            mockMvc.perform(get("/api/tours/published"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + pastTour.getId() + ")]").doesNotExist());
        }
    }

    // ==================== TOUR FILTER TESTS ====================

    @Nested
    @DisplayName("POST /api/tours/filter - Filter Tours")
    class FilterTourTests {

        @BeforeEach
        void setUpTestData() {
            // Create various tours for filtering
            createTourWithDetails("Greek Islands Cruise", TourCategory.CRUISE, TourStatus.PUBLISHED,
                    new BigDecimal("2500"), 7, "Istanbul", true, 30,
                    Arrays.asList("Athens", "Santorini", "Mykonos"));

            createTourWithDetails("Adventure in Alps", TourCategory.ADVENTURE, TourStatus.PUBLISHED,
                    new BigDecimal("1500"), 5, "Zurich", true, 20,
                    Arrays.asList("Zurich", "Interlaken", "Zermatt"));

            createTourWithDetails("Historical Rome", TourCategory.HISTORICAL, TourStatus.DRAFT,
                    new BigDecimal("1800"), 4, "Rome", true, 25,
                    Arrays.asList("Rome", "Vatican", "Pompeii"));

            createTourWithDetails("Budget Beach Tour", TourCategory.BEACH, TourStatus.PUBLISHED,
                    new BigDecimal("800"), 3, "Antalya", true, 50,
                    Arrays.asList("Antalya", "Side", "Alanya"));

            createTourWithDetails("Luxury Safari", TourCategory.SAFARI, TourStatus.PUBLISHED,
                    new BigDecimal("5000"), 10, "Nairobi", true, 15,
                    Arrays.asList("Nairobi", "Masai Mara", "Serengeti"));
        }

        @Test
        @DisplayName("Should filter by name (partial match)")
        void filterTours_ByName_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setName("Greek");

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", containsString("Greek")));
        }

        @Test
        @DisplayName("Should filter by name case-insensitively")
        void filterTours_ByNameCaseInsensitive_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setName("GREEK");

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("Should filter by single category")
        void filterTours_ByCategory_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setCategory(TourCategory.CRUISE);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].category", everyItem(equalTo("CRUISE"))));
        }

        @Test
        @DisplayName("Should filter by multiple categories")
        void filterTours_ByMultipleCategories_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setCategories(Arrays.asList(TourCategory.CRUISE, TourCategory.BEACH));

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].category",
                            everyItem(anyOf(equalTo("CRUISE"), equalTo("BEACH")))));
        }

        @Test
        @DisplayName("Should filter by status")
        void filterTours_ByStatus_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setStatus(TourStatus.DRAFT);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].status", everyItem(equalTo("DRAFT"))));
        }

        @Test
        @DisplayName("Should filter by price range")
        void filterTours_ByPriceRange_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setMinPrice(new BigDecimal("1000"));
            filter.setMaxPrice(new BigDecimal("2000"));

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].price",
                            everyItem(allOf(
                                    greaterThanOrEqualTo(1000.0),
                                    lessThanOrEqualTo(2000.0)
                            ))));
        }

        @Test
        @DisplayName("Should filter by minimum price only")
        void filterTours_ByMinPriceOnly_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setMinPrice(new BigDecimal("3000"));

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].price",
                            everyItem(greaterThanOrEqualTo(3000.0))));
        }

        @Test
        @DisplayName("Should filter by duration range")
        void filterTours_ByDurationRange_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setMinDuration(5);
            filter.setMaxDuration(8);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].duration",
                            everyItem(allOf(
                                    greaterThanOrEqualTo(5),
                                    lessThanOrEqualTo(8)
                            ))));
        }

        @Test
        @DisplayName("Should filter by departure city")
        void filterTours_ByDepartureCity_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setDepartureCity("Istanbul");

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].departureCity",
                            everyItem(containsStringIgnoringCase("Istanbul"))));
        }

        @Test
        @DisplayName("Should filter by destination")
        void filterTours_ByDestination_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setDestination("Santorini");

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].destinations", hasItem("Santorini")));
        }

        @Test
        @DisplayName("Should filter by isActive status")
        void filterTours_ByIsActive_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setIsActive(true);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].isActive", everyItem(equalTo(true))));
        }

        @Test
        @DisplayName("Should filter by isBookable composite condition")
        void filterTours_ByIsBookable_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setIsBookable(true);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].isActive", everyItem(equalTo(true))))
                    .andExpect(jsonPath("$.content[*].status", everyItem(equalTo("PUBLISHED"))))
                    .andExpect(jsonPath("$.content[*].availableSeats", everyItem(greaterThan(0))));
        }

        @Test
        @DisplayName("Should filter with multiple criteria")
        void filterTours_WithMultipleCriteria_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setCategory(TourCategory.CRUISE);
            filter.setMinPrice(new BigDecimal("2000"));
            filter.setIsActive(true);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].category", everyItem(equalTo("CRUISE"))))
                    .andExpect(jsonPath("$.content[*].price", everyItem(greaterThanOrEqualTo(2000.0))))
                    .andExpect(jsonPath("$.content[*].isActive", everyItem(equalTo(true))));
        }

        @Test
        @DisplayName("Should return empty results when no match")
        void filterTours_NoMatch_ShouldReturnEmpty() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setName("NonExistentTourName123");

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should sort by price ascending")
        void filterTours_SortByPriceAsc_ShouldReturnSorted() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setSortBy("price");
            filter.setSortDirection("ASC");

            MvcResult result = mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Verify prices are in ascending order
            String response = result.getResponse().getContentAsString();
            assertTrue(response.contains("content"));
        }

        @Test
        @DisplayName("Should sort by price descending")
        void filterTours_SortByPriceDesc_ShouldReturnSorted() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setSortBy("price");
            filter.setSortDirection("DESC");

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should paginate results")
        void filterTours_WithPagination_ShouldReturnPaginatedResults() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setPage(0);
            filter.setSize(2);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.totalPages").exists())
                    .andExpect(jsonPath("$.totalElements").exists());
        }

        @Test
        @DisplayName("Should handle second page request")
        void filterTours_SecondPage_ShouldReturnCorrectPage() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setPage(1);
            filter.setSize(2);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.hasPrevious").value(true));
        }

        @Test
        @DisplayName("Should limit max page size to 100")
        void filterTours_ExceedMaxPageSize_ShouldLimitTo100() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setSize(200);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(100));
        }

        @Test
        @DisplayName("Should use default sort when invalid sortBy provided")
        void filterTours_InvalidSortBy_ShouldUseDefault() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setSortBy("invalidField");

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should filter by hasAvailability")
        void filterTours_ByHasAvailability_ShouldReturnWithSeats() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setHasAvailability(true);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].availableSeats", everyItem(greaterThan(0))));
        }

        @Test
        @DisplayName("Should filter by available seats range")
        void filterTours_ByAvailableSeatsRange_ShouldReturnMatching() throws Exception {
            TourFilterDto filter = new TourFilterDto();
            filter.setMinAvailableSeats(20);
            filter.setMaxAvailableSeats(40);

            mockMvc.perform(post("/api/tours/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].availableSeats",
                            everyItem(allOf(
                                    greaterThanOrEqualTo(20),
                                    lessThanOrEqualTo(40)
                            ))));
        }

        private void createTourWithDetails(String name, TourCategory category, TourStatus status,
                                           BigDecimal price, int duration, String departureCity,
                                           boolean isActive, int maxParticipants, List<String> destinations) {
            Tour tour = new Tour();
            tour.setName(name);
            tour.setCategory(category);
            tour.setStatus(status);
            tour.setPrice(price);
            tour.setDuration(duration);
            tour.setDepartureCity(departureCity);
            tour.setIsActive(isActive);
            tour.setMaxParticipants(maxParticipants);
            tour.setMinParticipants(1);
            tour.setAvailableSeats(maxParticipants);
            tour.setDestinations(destinations);
            tour.setStartDate(LocalDateTime.now().plusDays(30));
            tour.setEndDate(LocalDateTime.now().plusDays(30 + duration));
            tour.setBookingDeadline(LocalDateTime.now().plusDays(25));
            tour.setCreatedBy(adminUser.getId());
            tourRepository.save(tour);
        }
    }

    // ==================== SEARCH ENDPOINT TESTS ====================

    @Nested
    @DisplayName("GET /api/tours/search - Search Tours")
    class SearchTourTests {

        @Test
        @DisplayName("Should search tours using query parameters")
        void searchTours_WithQueryParams_ShouldWork() throws Exception {
            Tour tour = createAndSaveTour();
            tour.setStatus(TourStatus.PUBLISHED);
            tour.setIsActive(true);
            tourRepository.save(tour);

            mockMvc.perform(get("/api/tours/search")
                            .param("isActive", "true")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("Should search tours by category parameter")
        void searchTours_ByCategory_ShouldWork() throws Exception {
            Tour cruiseTour = createAndSaveTour();
            cruiseTour.setCategory(TourCategory.CRUISE);
            tourRepository.save(cruiseTour);

            mockMvc.perform(get("/api/tours/search")
                            .param("category", "CRUISE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].category", everyItem(equalTo("CRUISE"))));
        }

        @Test
        @DisplayName("Should search tours by price range parameters")
        void searchTours_ByPriceRange_ShouldWork() throws Exception {
            Tour tour = createAndSaveTour();
            tour.setPrice(new BigDecimal("1500"));
            tourRepository.save(tour);

            mockMvc.perform(get("/api/tours/search")
                            .param("minPrice", "1000")
                            .param("maxPrice", "2000"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle invalid enum value gracefully")
        void searchTours_WithInvalidCategory_ShouldFail() throws Exception {
            mockMvc.perform(get("/api/tours/search")
                            .param("category", "INVALID_CATEGORY"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== HELPER METHODS ====================

    private TourCreateDto createValidTourDto() {
        TourCreateDto dto = new TourCreateDto();
        dto.setName("Test Tour");
        dto.setPrice(new BigDecimal("1500"));
        dto.setDestinations(Arrays.asList("Paris", "London", "Rome"));
        dto.setDepartureCity("Istanbul");
        dto.setDuration(7);
        dto.setMinParticipants(10);
        dto.setMaxParticipants(30);
        dto.setStartDate(LocalDateTime.now().plusDays(30));
        dto.setEndDate(LocalDateTime.now().plusDays(37));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(25));
        dto.setCategory(TourCategory.CULTURAL);
        return dto;
    }

    private Tour createAndSaveTour() {
        Tour tour = new Tour();
        tour.setName("Test Tour " + System.currentTimeMillis());
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
        tour.setCreatedBy(adminUser.getId());
        return tourRepository.save(tour);
    }
}