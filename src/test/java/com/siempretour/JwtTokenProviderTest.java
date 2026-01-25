package com.siempretour;

import com.siempretour.Security.JwtTokenProvider;
import com.siempretour.User.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@AllArgsConstructor
class JwtTokenProviderTest {


    private final JwtTokenProvider jwtTokenProvider;

    // ==================== TOKEN CREATION TESTS ====================

    @Nested
    @DisplayName("Token Creation")
    class TokenCreationTests {

        @Test
        @DisplayName("Should create valid token")
        void createToken_ShouldReturnValidToken() {
            String token = jwtTokenProvider.createToken("test@example.com", 1L, UserRole.USER.name());

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Should include email in token")
        void createToken_ShouldIncludeEmail() {
            String email = "test@example.com";
            String token = jwtTokenProvider.createToken(email, 1L, UserRole.USER.name());

            String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
            assertEquals(email, extractedEmail);
        }

        @Test
        @DisplayName("Should include userId in token")
        void createToken_ShouldIncludeUserId() {
            Long userId = 12345L;
            String token = jwtTokenProvider.createToken("test@example.com", userId, UserRole.USER.name());

            Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
            assertEquals(userId, extractedUserId);
        }

        @Test
        @DisplayName("Should include role in token")
        void createToken_ShouldIncludeRole() {
            String role = UserRole.ADMIN.name();
            String token = jwtTokenProvider.createToken("test@example.com", 1L, role);

            String extractedRole = jwtTokenProvider.getRoleFromToken(token);
            assertEquals(role, extractedRole);
        }

        @Test
        @DisplayName("Should create different tokens for different users")
        void createToken_DifferentUsers_ShouldHaveDifferentTokens() {
            String token1 = jwtTokenProvider.createToken("user1@example.com", 1L, UserRole.USER.name());
            String token2 = jwtTokenProvider.createToken("user2@example.com", 2L, UserRole.USER.name());

            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("Should create different tokens for different roles")
        void createToken_DifferentRoles_ShouldHaveDifferentTokens() {
            String userToken = jwtTokenProvider.createToken("test@example.com", 1L, UserRole.USER.name());
            String adminToken = jwtTokenProvider.createToken("test@example.com", 1L, UserRole.ADMIN.name());

            assertNotEquals(userToken, adminToken);
        }
    }

    // ==================== TOKEN VALIDATION TESTS ====================

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct token")
        void validateToken_CorrectToken_ShouldReturnTrue() {
            String token = jwtTokenProvider.createToken("test@example.com", 1L, UserRole.USER.name());

            boolean isValid = jwtTokenProvider.validateToken(token);

            assertTrue(isValid);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should reject null or empty token")
        void validateToken_NullOrEmpty_ShouldReturnFalse(String token) {
            boolean isValid = jwtTokenProvider.validateToken(token);

            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should reject malformed token")
        void validateToken_MalformedToken_ShouldReturnFalse() {
            boolean isValid = jwtTokenProvider.validateToken("not.a.valid.jwt.token");

            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void validateToken_InvalidSignature_ShouldReturnFalse() {
            String token = jwtTokenProvider.createToken("test@example.com", 1L, UserRole.USER.name());
            // Modify the signature part
            String tamperedToken = token.substring(0, token.lastIndexOf('.')) + ".invalidsignature";

            boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

            assertFalse(isValid);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "random-string",
                "eyJhbGciOiJIUzI1NiJ9",
                "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0",
                "a.b.c.d.e"
        })
        @DisplayName("Should reject various invalid token formats")
        void validateToken_VariousInvalidFormats_ShouldReturnFalse(String invalidToken) {
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            assertFalse(isValid);
        }
    }

    // ==================== CLAIM EXTRACTION TESTS ====================

    @Nested
    @DisplayName("Claim Extraction")
    class ClaimExtractionTests {

        @Test
        @DisplayName("Should extract email from valid token")
        void getEmailFromToken_ValidToken_ShouldReturnEmail() {
            String email = "extract@example.com";
            String token = jwtTokenProvider.createToken(email, 1L, UserRole.USER.name());

            String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

            assertEquals(email, extractedEmail);
        }

        @Test
        @DisplayName("Should extract userId from valid token")
        void getUserIdFromToken_ValidToken_ShouldReturnUserId() {
            Long userId = 99999L;
            String token = jwtTokenProvider.createToken("test@example.com", userId, UserRole.USER.name());

            Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            assertEquals(userId, extractedUserId);
        }

        @Test
        @DisplayName("Should extract role from valid token")
        void getRoleFromToken_ValidToken_ShouldReturnRole() {
            String role = UserRole.ADMIN.name();
            String token = jwtTokenProvider.createToken("test@example.com", 1L, role);

            String extractedRole = jwtTokenProvider.getRoleFromToken(token);

            assertEquals(role, extractedRole);
        }

        @Test
        @DisplayName("Should throw exception for email extraction from invalid token")
        void getEmailFromToken_InvalidToken_ShouldThrowException() {
            assertThrows(Exception.class, () -> {
                jwtTokenProvider.getEmailFromToken("invalid-token");
            });
        }

        @Test
        @DisplayName("Should throw exception for userId extraction from invalid token")
        void getUserIdFromToken_InvalidToken_ShouldThrowException() {
            assertThrows(Exception.class, () -> {
                jwtTokenProvider.getUserIdFromToken("invalid-token");
            });
        }

        @Test
        @DisplayName("Should throw exception for role extraction from invalid token")
        void getRoleFromToken_InvalidToken_ShouldThrowException() {
            assertThrows(Exception.class, () -> {
                jwtTokenProvider.getRoleFromToken("invalid-token");
            });
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle special characters in email")
        void createToken_SpecialCharsInEmail_ShouldWork() {
            String email = "test+special.chars@sub.example.com";
            String token = jwtTokenProvider.createToken(email, 1L, UserRole.USER.name());

            assertTrue(jwtTokenProvider.validateToken(token));
            assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
        }

        @Test
        @DisplayName("Should handle very long email")
        void createToken_LongEmail_ShouldWork() {
            String email = "very.long.email.address.that.goes.on.and.on@subdomain.example.com";
            String token = jwtTokenProvider.createToken(email, 1L, UserRole.USER.name());

            assertTrue(jwtTokenProvider.validateToken(token));
            assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
        }

        @Test
        @DisplayName("Should handle large userId")
        void createToken_LargeUserId_ShouldWork() {
            Long userId = Long.MAX_VALUE;
            String token = jwtTokenProvider.createToken("test@example.com", userId, UserRole.USER.name());

            assertTrue(jwtTokenProvider.validateToken(token));
            assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("Should handle minimum userId")
        void createToken_MinimumUserId_ShouldWork() {
            Long userId = 1L;
            String token = jwtTokenProvider.createToken("test@example.com", userId, UserRole.USER.name());

            assertTrue(jwtTokenProvider.validateToken(token));
            assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        }
    }
}