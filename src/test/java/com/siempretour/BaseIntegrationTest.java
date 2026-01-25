package com.siempretour;

import com.siempretour.Security.JwtTokenProvider;
import com.siempretour.User.UserEntity;
import com.siempretour.User.UserRole;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AllArgsConstructor
public abstract class BaseIntegrationTest {


    protected final MockMvc mockMvc;

    protected final JwtTokenProvider jwtTokenProvider;

    protected String generateUserToken(Long userId, String email) {
        return jwtTokenProvider.createToken(email, userId, UserRole.USER.name());
    }

    protected String generateAdminToken(Long userId, String email) {
        return jwtTokenProvider.createToken(email, userId, UserRole.ADMIN.name());
    }

    protected UserEntity createTestUser(Long id, String email, UserRole role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("encoded_password");
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(true);
        return user;
    }
}