package com.siempretour.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/google",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/health", "/uploads/**").permitAll()

                        // Public tour endpoints (including filter/search for browsing)
                        .requestMatchers("/api/tours/published", "/api/tours/active").permitAll()
                        .requestMatchers("/api/tours/filter", "/api/tours/filter/all", "/api/tours/search").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tours/{id}").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tours/by-destination").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tours/by-destination/paged").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tours/by-slug/**").permitAll()

                        // Public homepage config (drives index.html dynamic sections)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/homepage").permitAll()

                        // Public review endpoints
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/by-tour/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/by-destination").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/reviews").permitAll()

                        // Contact form (public)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/contact").permitAll()

                        // Public chat widget endpoint
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/chat").permitAll()

                        // Admin-only booking endpoints (must precede the broader authenticated rules
                        // so that GET /api/bookings (list-all) is locked down to admins. The exact-match
                        // pattern "/api/bookings" only catches the root collection; nested user routes
                        // like /api/bookings/me and /api/bookings/{id} are handled below.)
                        .requestMatchers("/api/bookings/pending", "/api/bookings/all", "/api/bookings/tour/**").hasRole("ADMIN")
                        .requestMatchers("/api/bookings/{id}/approve", "/api/bookings/{id}/reject").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/bookings").hasRole("ADMIN")

                        // User endpoints (authenticated)
                        .requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
                        .requestMatchers("/api/bookings/my-bookings", "/api/bookings/me", "/api/bookings/{id}").authenticated()
                        // POST /api/bookings (create a booking) requires a logged-in user, not admin.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/bookings").authenticated()

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tours/bulk-import").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tours").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/tours/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/tours/**").hasRole("ADMIN")
                        .requestMatchers("/api/tours/my-tours").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/pending").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/reviews/{id}/approve", "/api/reviews/{id}/reject").hasRole("ADMIN")
                        .requestMatchers("/api/auth/users/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
