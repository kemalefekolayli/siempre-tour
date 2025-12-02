package Security;


import Exceptions.ErrorCodes;
import Exceptions.GlobalException;
import User.Dto.SupabaseUserInfo;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class    JwtHelper implements Converter<Jwt, Collection<GrantedAuthority>> {

    /**
     * Convert JWT to Spring Security authorities
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String role = jwt.getClaim("role");

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }

    /**
     * Extract Supabase user info from current JWT token
     */
    public SupabaseUserInfo getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            return SupabaseUserInfo.builder()
                    .userId(jwt.getSubject())
                    .email(jwt.getClaim("email"))
                    .role(jwt.getClaim("role"))
                    .userMetadata(jwt.getClaim("user_metadata"))
                    .appMetadata(jwt.getClaim("app_metadata"))
                    .build();
        }

        throw new GlobalException(ErrorCodes.AUTH_TOKEN_INVALID);
    }

    /**
     * Get just the Supabase user ID from token
     */
    public String getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * Get user email from token
     */
    public String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    /**
     * Check if current user has specific role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role.toUpperCase()));
    }
}