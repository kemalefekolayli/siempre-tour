package com.siempretour.User;

import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Security.JwtHelper;
import com.siempretour.Security.JwtTokenProvider;
import com.siempretour.User.Dto.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserEntityRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtHelper jwtHelper;
    private final JavaMailSender mailSender;

    @Value("${google.oauth.client-ids:${GOOGLE_OAUTH_CLIENT_IDS:}}")
    private String googleClientIds;

    @Value("${app.frontend.base-url:http://localhost:5500}")
    private String frontendBaseUrl;

    @Value("${auth.password-reset.path:/login.html?resetToken=}")
    private String passwordResetPath;

    @Value("${auth.password-reset.token-validity-minutes:30}")
    private long passwordResetTokenValidityMinutes;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GlobalException(ErrorCodes.AUTH_EMAIL_TAKEN);
        }

        // Create user
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt hash
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setEmailVerified(false);

        UserEntity savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new GlobalException(ErrorCodes.AUTH_INVALID_CREDENTIALS));

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new GlobalException(ErrorCodes.AUTH_USER_NOT_FOUND); // Don't reveal it's locked
        }

        // Check if account is active
        if (!user.getIsActive()) {
            throw new GlobalException(ErrorCodes.AUTH_USER_NOT_FOUND);
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            throw new GlobalException(ErrorCodes.AUTH_INVALID_CREDENTIALS);
        }

        // Reset failed attempts and update last login
        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getCredential());

        String email = normalizeEmail(payload.getEmail());
        String googleId = payload.getSubject();

        if (isBlank(email) || isBlank(googleId) || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new GlobalException(ErrorCodes.AUTH_GOOGLE_TOKEN_INVALID);
        }

        UserEntity user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElseGet(() -> createGoogleUser(payload, email, googleId));

        if (!user.getIsActive()) {
            throw new GlobalException(ErrorCodes.AUTH_USER_NOT_FOUND);
        }

        if (user.isAccountLocked()) {
            throw new GlobalException(ErrorCodes.AUTH_ACCOUNT_LOCKED);
        }

        if (isBlank(user.getGoogleId())) {
            user.setGoogleId(googleId);
        }

        user.setEmailVerified(true);
        user.setAvatarUrl(claimToString(payload, "picture"));
        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);
        log.info("User logged in with Google: {}", savedUser.getEmail());

        return buildAuthResponse(savedUser);
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.getIsActive()) {
                return;
            }

            String resetToken = generateSecureToken();
            user.setPasswordResetTokenHash(hashToken(resetToken));
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(passwordResetTokenValidityMinutes));
            userRepository.save(user);

            sendPasswordResetEmail(user, resetToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.getToken());
        UserEntity user = userRepository.findByPasswordResetTokenHash(tokenHash)
                .orElseThrow(() -> new GlobalException(ErrorCodes.AUTH_RESET_TOKEN_INVALID));

        if (user.getPasswordResetTokenExpiresAt() == null ||
                user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            clearPasswordResetToken(user);
            userRepository.save(user);
            throw new GlobalException(ErrorCodes.AUTH_RESET_TOKEN_INVALID);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        clearPasswordResetToken(user);
        user.resetFailedAttempts();
        userRepository.save(user);

        log.info("Password reset completed for user: {}", user.getEmail());
    }

    public UserResponseDto getCurrentUser() {
        Long userId = jwtHelper.getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.AUTH_USER_NOT_FOUND));

        return mapToUserResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = jwtHelper.getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.AUTH_USER_NOT_FOUND));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new GlobalException(ErrorCodes.AUTH_INVALID_CREDENTIALS);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String token = tokenProvider.createToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String credential) {
        List<String> audiences = Arrays.stream(googleClientIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        if (audiences.isEmpty()) {
            throw new GlobalException(ErrorCodes.AUTH_GOOGLE_CLIENT_NOT_CONFIGURED);
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(audiences)
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new GlobalException(ErrorCodes.AUTH_GOOGLE_TOKEN_INVALID);
            }

            return idToken.getPayload();
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            throw new GlobalException(ErrorCodes.AUTH_GOOGLE_TOKEN_INVALID);
        }
    }

    private UserEntity createGoogleUser(GoogleIdToken.Payload payload, String email, String googleId) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("google:" + UUID.randomUUID()));
        user.setFirstName(resolveGoogleFirstName(payload, email));
        user.setLastName(resolveGoogleLastName(payload));
        user.setPhoneNumber(null);
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setGoogleId(googleId);
        user.setAvatarUrl(claimToString(payload, "picture"));
        user.setLastLoginAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);
        log.info("New Google user registered: {}", savedUser.getEmail());
        return savedUser;
    }

    private String resolveGoogleFirstName(GoogleIdToken.Payload payload, String email) {
        String givenName = claimToString(payload, "given_name");
        if (!isBlank(givenName)) {
            return trimToLength(givenName, 50);
        }

        String name = claimToString(payload, "name");
        if (!isBlank(name)) {
            return trimToLength(name.split("\\s+")[0], 50);
        }

        return trimToLength(email.substring(0, email.indexOf("@")), 50);
    }

    private String resolveGoogleLastName(GoogleIdToken.Payload payload) {
        String familyName = claimToString(payload, "family_name");
        if (!isBlank(familyName)) {
            return trimToLength(familyName, 50);
        }

        String name = claimToString(payload, "name");
        if (!isBlank(name)) {
            String[] parts = name.trim().split("\\s+", 2);
            if (parts.length > 1) {
                return trimToLength(parts[1], 50);
            }
        }

        return "Google";
    }

    private void sendPasswordResetEmail(UserEntity user, String resetToken) {
        if (isBlank(mailUsername)) {
            log.warn("Mail username is not configured, skipping password reset email for {}", user.getEmail());
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject("Siempre Tour sifre sifirlama");
            mail.setText("""
                    Merhaba %s,

                    Siempre Tour hesabiniz icin sifre sifirlama talebi aldik.
                    Yeni sifre belirlemek icin asagidaki baglantiyi kullanin:

                    %s

                    Bu baglanti %d dakika boyunca gecerlidir. Bu talebi siz yapmadiysaniz bu e-postayi yok sayabilirsiniz.
                    """.formatted(user.getFirstName(), buildPasswordResetLink(resetToken), passwordResetTokenValidityMinutes));

            mailSender.send(mail);
            log.info("Password reset email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildPasswordResetLink(String resetToken) {
        String baseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        String path = passwordResetPath.startsWith("/") ? passwordResetPath : "/" + passwordResetPath;
        String encodedToken = URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
        return baseUrl + path + encodedToken;
    }

    private void clearPasswordResetToken(UserEntity user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String claimToString(GoogleIdToken.Payload payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToLength(String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private UserResponseDto mapToUserResponse(UserEntity user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        return dto;
    }
}
