package siempretour.User;

import com.siempretour.User.UserEntity;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = createSampleUser();
    }

    // ==================== isAccountLocked TESTS ====================

    @Nested
    @DisplayName("isAccountLocked")
    class IsAccountLockedTests {

        @Test
        @DisplayName("Should not be locked when lockedUntil is null")
        void isAccountLocked_NullLockedUntil_ShouldReturnFalse() {
            user.setLockedUntil(null);
            assertFalse(user.isAccountLocked());
        }

        @Test
        @DisplayName("Should be locked when lockedUntil is in future")
        void isAccountLocked_FutureLockedUntil_ShouldReturnTrue() {
            user.setLockedUntil(LocalDateTime.now().plusHours(1));
            assertTrue(user.isAccountLocked());
        }

        @Test
        @DisplayName("Should not be locked when lockedUntil is in past")
        void isAccountLocked_PastLockedUntil_ShouldReturnFalse() {
            user.setLockedUntil(LocalDateTime.now().minusHours(1));
            assertFalse(user.isAccountLocked());
        }

        @Test
        @DisplayName("Should handle edge case of lockedUntil just now")
        void isAccountLocked_ExactlyNow_ShouldReturnFalse() {
            user.setLockedUntil(LocalDateTime.now());
            assertFalse(user.isAccountLocked());
        }
    }

    // ==================== incrementFailedAttempts TESTS ====================

    @Nested
    @DisplayName("incrementFailedAttempts")
    class IncrementFailedAttemptsTests {

        @Test
        @DisplayName("Should increment failed attempts by 1")
        void incrementFailedAttempts_ShouldIncrementByOne() {
            user.setFailedLoginAttempts(2);
            user.incrementFailedAttempts();
            assertEquals(3, user.getFailedLoginAttempts());
        }

        @Test
        @DisplayName("Should lock account after 5 failed attempts")
        void incrementFailedAttempts_FifthAttempt_ShouldLock() {
            user.setFailedLoginAttempts(4);
            user.incrementFailedAttempts();

            assertEquals(5, user.getFailedLoginAttempts());
            assertNotNull(user.getLockedUntil());
            assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now().plusMinutes(29)));
        }

        @Test
        @DisplayName("Should set lock duration to 30 minutes")
        void incrementFailedAttempts_LockDuration_ShouldBe30Minutes() {
            user.setFailedLoginAttempts(4);
            LocalDateTime beforeLock = LocalDateTime.now();

            user.incrementFailedAttempts();

            assertTrue(user.getLockedUntil().isAfter(beforeLock.plusMinutes(29)));
            assertTrue(user.getLockedUntil().isBefore(beforeLock.plusMinutes(31)));
        }

        @Test
        @DisplayName("Should not lock before 5 attempts")
        void incrementFailedAttempts_LessThan5_ShouldNotLock() {
            user.setFailedLoginAttempts(3);
            user.incrementFailedAttempts();

            assertEquals(4, user.getFailedLoginAttempts());
            assertNull(user.getLockedUntil());
        }

        @Test
        @DisplayName("Should handle increment from zero")
        void incrementFailedAttempts_FromZero_ShouldBecomeOne() {
            user.setFailedLoginAttempts(0);
            user.incrementFailedAttempts();
            assertEquals(1, user.getFailedLoginAttempts());
        }
    }

    // ==================== resetFailedAttempts TESTS ====================

    @Nested
    @DisplayName("resetFailedAttempts")
    class ResetFailedAttemptsTests {

        @Test
        @DisplayName("Should reset failed attempts to zero")
        void resetFailedAttempts_ShouldSetToZero() {
            user.setFailedLoginAttempts(4);
            user.resetFailedAttempts();
            assertEquals(0, user.getFailedLoginAttempts());
        }

        @Test
        @DisplayName("Should clear lockedUntil")
        void resetFailedAttempts_ShouldClearLockedUntil() {
            user.setFailedLoginAttempts(5);
            user.setLockedUntil(LocalDateTime.now().plusHours(1));

            user.resetFailedAttempts();

            assertNull(user.getLockedUntil());
        }

        @Test
        @DisplayName("Should handle reset when already zero")
        void resetFailedAttempts_AlreadyZero_ShouldStayZero() {
            user.setFailedLoginAttempts(0);
            user.resetFailedAttempts();
            assertEquals(0, user.getFailedLoginAttempts());
        }

        @Test
        @DisplayName("Should handle reset when lockedUntil already null")
        void resetFailedAttempts_LockedUntilAlreadyNull_ShouldStayNull() {
            user.setFailedLoginAttempts(3);
            user.setLockedUntil(null);

            user.resetFailedAttempts();

            assertNull(user.getLockedUntil());
            assertEquals(0, user.getFailedLoginAttempts());
        }
    }

    // ==================== getFullName TESTS ====================

    @Nested
    @DisplayName("getFullName")
    class GetFullNameTests {

        @Test
        @DisplayName("Should return combined first and last name")
        void getFullName_ShouldReturnCombined() {
            user.setFirstName("John");
            user.setLastName("Doe");

            assertEquals("John Doe", user.getFullName());
        }

        @Test
        @DisplayName("Should handle single character names")
        void getFullName_SingleCharNames_ShouldWork() {
            user.setFirstName("J");
            user.setLastName("D");

            assertEquals("J D", user.getFullName());
        }

        @Test
        @DisplayName("Should handle names with spaces")
        void getFullName_NamesWithSpaces_ShouldWork() {
            user.setFirstName("Mary Jane");
            user.setLastName("Watson Parker");

            assertEquals("Mary Jane Watson Parker", user.getFullName());
        }
    }

    // ==================== LIFECYCLE CALLBACK TESTS ====================

    @Nested
    @DisplayName("Lifecycle Callbacks")
    class LifecycleCallbackTests {

        @Test
        @DisplayName("onCreate should set timestamps")
        void onCreate_ShouldSetTimestamps() {
            UserEntity newUser = new UserEntity();
            newUser.setEmail("test@example.com");
            newUser.setPassword("password");
            newUser.setFirstName("Test");
            newUser.setLastName("User");

            newUser.onCreate();

            assertNotNull(newUser.getCreatedAt());
            assertNotNull(newUser.getUpdatedAt());
            assertEquals(newUser.getCreatedAt(), newUser.getUpdatedAt());
        }

        @Test
        @DisplayName("onUpdate should update updatedAt timestamp")
        void onUpdate_ShouldUpdateTimestamp() throws InterruptedException {
            user.onCreate();
            LocalDateTime originalUpdatedAt = user.getUpdatedAt();

            Thread.sleep(10);
            user.onUpdate();

            assertTrue(user.getUpdatedAt().isAfter(originalUpdatedAt));
        }

        @Test
        @DisplayName("onUpdate should not change createdAt")
        void onUpdate_ShouldNotChangeCreatedAt() throws InterruptedException {
            user.onCreate();
            LocalDateTime originalCreatedAt = user.getCreatedAt();

            Thread.sleep(10);
            user.onUpdate();

            assertEquals(originalCreatedAt, user.getCreatedAt());
        }
    }

    // ==================== DEFAULT VALUES TESTS ====================

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have default role as USER")
        void defaultRole_ShouldBeUser() {
            UserEntity newUser = new UserEntity();
            assertEquals(UserRole.USER, newUser.getRole());
        }

        @Test
        @DisplayName("Should have default isActive as true")
        void defaultIsActive_ShouldBeTrue() {
            UserEntity newUser = new UserEntity();
            assertTrue(newUser.getIsActive());
        }

        @Test
        @DisplayName("Should have default emailVerified as false")
        void defaultEmailVerified_ShouldBeFalse() {
            UserEntity newUser = new UserEntity();
            assertFalse(newUser.getEmailVerified());
        }

        @Test
        @DisplayName("Should have default failedLoginAttempts as 0")
        void defaultFailedLoginAttempts_ShouldBeZero() {
            UserEntity newUser = new UserEntity();
            assertEquals(0, newUser.getFailedLoginAttempts());
        }
    }

    // ==================== HELPER METHODS ====================

    private UserEntity createSampleUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encoded_password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        return user;
    }
}