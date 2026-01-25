package siempretour.User;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.siempretour.Security.JwtTokenProvider;
import com.siempretour.User.Dto.*;

import com.siempretour.User.UserEntityRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ==================== REGISTRATION TESTS ====================

    @Nested
    @DisplayName("POST /api/auth/register - User Registration")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user successfully with valid data")
        void register_WithValidData_ShouldSucceed() throws Exception {
            RegisterRequest request = createValidRegisterRequest();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.email").value(request.getEmail()))
                    .andExpect(jsonPath("$.firstName").value(request.getFirstName()))
                    .andExpect(jsonPath("$.lastName").value(request.getLastName()))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("Should reject duplicate email registration")
        void register_DuplicateEmail_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();

            // First registration
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Duplicate registration
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with missing email")
        void register_MissingEmail_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setEmail(null);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with invalid email format")
        void register_InvalidEmail_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setEmail("invalid-email");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-an-email", "missing@", "@nodomain"})
        @DisplayName("Should reject registration with various invalid email formats")
        void register_VariousInvalidEmails_ShouldFail(String invalidEmail) throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setEmail(invalidEmail);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with missing password")
        void register_MissingPassword_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPassword(null);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with short password")
        void register_ShortPassword_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPassword("Abc@1");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with password missing uppercase")
        void register_PasswordMissingUppercase_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPassword("password@123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with password missing lowercase")
        void register_PasswordMissingLowercase_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPassword("PASSWORD@123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with password missing digit")
        void register_PasswordMissingDigit_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPassword("Password@abc");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with password missing special character")
        void register_PasswordMissingSpecialChar_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPassword("Password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with missing first name")
        void register_MissingFirstName_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setFirstName(null);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with too short first name")
        void register_TooShortFirstName_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setFirstName("A");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with missing last name")
        void register_MissingLastName_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setLastName(null);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should accept registration with valid phone number")
        void register_ValidPhoneNumber_ShouldSucceed() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPhoneNumber("5551234567");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should reject registration with invalid phone number")
        void register_InvalidPhoneNumber_ShouldFail() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPhoneNumber("123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== LOGIN TESTS ====================

    @Nested
    @DisplayName("POST /api/auth/login - User Login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_WithValidCredentials_ShouldSucceed() throws Exception {
            UserEntity user = createAndSaveUser("login@test.com", "Password@123");

            LoginRequest request = new LoginRequest();
            request.setEmail("login@test.com");
            request.setPassword("Password@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.email").value("login@test.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("Should reject login with wrong password")
        void login_WrongPassword_ShouldFail() throws Exception {
            createAndSaveUser("login2@test.com", "Password@123");

            LoginRequest request = new LoginRequest();
            request.setEmail("login2@test.com");
            request.setPassword("WrongPassword@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject login with non-existent email")
        void login_NonExistentEmail_ShouldFail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("nonexistent@test.com");
            request.setPassword("Password@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject login with missing email")
        void login_MissingEmail_ShouldFail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setPassword("Password@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject login with missing password")
        void login_MissingPassword_ShouldFail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@test.com");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject login for inactive user")
        void login_InactiveUser_ShouldFail() throws Exception {
            UserEntity user = createAndSaveUser("inactive@test.com", "Password@123");
            user.setIsActive(false);
            userRepository.save(user);

            LoginRequest request = new LoginRequest();
            request.setEmail("inactive@test.com");
            request.setPassword("Password@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject login for locked account")
        void login_LockedAccount_ShouldFail() throws Exception {
            UserEntity user = createAndSaveUser("locked@test.com", "Password@123");
            user.setLockedUntil(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            LoginRequest request = new LoginRequest();
            request.setEmail("locked@test.com");
            request.setPassword("Password@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should increment failed attempts on wrong password")
        void login_WrongPassword_ShouldIncrementFailedAttempts() throws Exception {
            UserEntity user = createAndSaveUser("attempts@test.com", "Password@123");

            LoginRequest request = new LoginRequest();
            request.setEmail("attempts@test.com");
            request.setPassword("WrongPassword@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            UserEntity updatedUser = userRepository.findByEmail("attempts@test.com").orElseThrow();
            assertEquals(1, updatedUser.getFailedLoginAttempts());
        }

        @Test
        @DisplayName("Should lock account after 5 failed attempts")
        void login_FiveFailedAttempts_ShouldLockAccount() throws Exception {
            UserEntity user = createAndSaveUser("locktest@test.com", "Password@123");
            user.setFailedLoginAttempts(4);
            userRepository.save(user);

            LoginRequest request = new LoginRequest();
            request.setEmail("locktest@test.com");
            request.setPassword("WrongPassword@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            UserEntity updatedUser = userRepository.findByEmail("locktest@test.com").orElseThrow();
            assertEquals(5, updatedUser.getFailedLoginAttempts());
            assertNotNull(updatedUser.getLockedUntil());
        }

        @Test
        @DisplayName("Should reset failed attempts on successful login")
        void login_Successful_ShouldResetFailedAttempts() throws Exception {
            UserEntity user = createAndSaveUser("reset@test.com", "Password@123");
            user.setFailedLoginAttempts(3);
            userRepository.save(user);

            LoginRequest request = new LoginRequest();
            request.setEmail("reset@test.com");
            request.setPassword("Password@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            UserEntity updatedUser = userRepository.findByEmail("reset@test.com").orElseThrow();
            assertEquals(0, updatedUser.getFailedLoginAttempts());
            assertNull(updatedUser.getLockedUntil());
        }

        @Test
        @DisplayName("Should update last login timestamp on successful login")
        void login_Successful_ShouldUpdateLastLogin() throws Exception {
            UserEntity user = createAndSaveUser("lastlogin@test.com", "Password@123");
            LocalDateTime beforeLogin = LocalDateTime.now().minusSeconds(1);

            LoginRequest request = new LoginRequest();
            request.setEmail("lastlogin@test.com");
            request.setPassword("Password@123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            UserEntity updatedUser = userRepository.findByEmail("lastlogin@test.com").orElseThrow();
            assertNotNull(updatedUser.getLastLoginAt());
            assertTrue(updatedUser.getLastLoginAt().isAfter(beforeLogin));
        }
    }

    // ==================== GET CURRENT USER TESTS ====================

    @Nested
    @DisplayName("GET /api/auth/me - Get Current User")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user info with valid token")
        void getCurrentUser_WithValidToken_ShouldSucceed() throws Exception {
            UserEntity user = createAndSaveUser("me@test.com", "Password@123");
            String token = jwtTokenProvider.createToken(user.getEmail(), user.getId(), user.getRole().name());

            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                    .andExpect(jsonPath("$.lastName").value(user.getLastName()))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("Should reject request without token")
        void getCurrentUser_WithoutToken_ShouldFail() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject request with invalid token")
        void getCurrentUser_WithInvalidToken_ShouldFail() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject request with malformed authorization header")
        void getCurrentUser_MalformedHeader_ShouldFail() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "NotBearer token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Nested
    @DisplayName("POST /api/auth/change-password - Change Password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void changePassword_WithValidData_ShouldSucceed() throws Exception {
            UserEntity user = createAndSaveUser("changepass@test.com", "OldPassword@123");
            String token = jwtTokenProvider.createToken(user.getEmail(), user.getId(), user.getRole().name());

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("OldPassword@123");
            request.setNewPassword("NewPassword@456");

            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Verify new password works
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("changepass@test.com");
            loginRequest.setPassword("NewPassword@456");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject change with wrong current password")
        void changePassword_WrongCurrentPassword_ShouldFail() throws Exception {
            UserEntity user = createAndSaveUser("wrongcurrent@test.com", "CurrentPassword@123");
            String token = jwtTokenProvider.createToken(user.getEmail(), user.getId(), user.getRole().name());

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("WrongCurrent@123");
            request.setNewPassword("NewPassword@456");

            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject change with weak new password")
        void changePassword_WeakNewPassword_ShouldFail() throws Exception {
            UserEntity user = createAndSaveUser("weaknew@test.com", "CurrentPassword@123");
            String token = jwtTokenProvider.createToken(user.getEmail(), user.getId(), user.getRole().name());

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("CurrentPassword@123");
            request.setNewPassword("weak");

            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject change without authentication")
        void changePassword_WithoutAuth_ShouldFail() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("Current@123");
            request.setNewPassword("NewPassword@456");

            mockMvc.perform(post("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject change with missing current password")
        void changePassword_MissingCurrentPassword_ShouldFail() throws Exception {
            UserEntity user = createAndSaveUser("missingcurrent@test.com", "Password@123");
            String token = jwtTokenProvider.createToken(user.getEmail(), user.getId(), user.getRole().name());

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setNewPassword("NewPassword@456");

            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject change with missing new password")
        void changePassword_MissingNewPassword_ShouldFail() throws Exception {
            UserEntity user = createAndSaveUser("missingnew@test.com", "Password@123");
            String token = jwtTokenProvider.createToken(user.getEmail(), user.getId(), user.getRole().name());

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("Password@123");

            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== HELPER METHODS ====================

    private RegisterRequest createValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test" + System.currentTimeMillis() + "@example.com");
        request.setPassword("Password@123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhoneNumber("5551234567");
        return request;
    }

    private UserEntity createAndSaveUser(String email, String password) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}