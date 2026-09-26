package com.manatandas.backend.config;

import com.manatandas.backend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // The JWT now lives in an httpOnly cookie, which normally means
            // CSRF risk is back on the table (browsers auto-attach cookies,
            // even to requests forged by another site). It stays disabled
            // here because the cookie is set with SameSite=Lax (see
            // AuthCookieService) -- browsers simply won't attach a Lax
            // cookie to a genuine cross-site request in the first place, so
            // there's nothing left for a CSRF token to additionally protect
            // against for this API today.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // /me deliberately NOT included here -- it needs a valid
                // cookie to answer "who am I", so it falls through to
                // anyRequest().authenticated() below.
                .requestMatchers("/api/auth/google", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bathrooms/**").permitAll()
                // Bulk imports from OSM etc. stay open for now — this is a
                // manual dev/admin action, not a user-facing feature yet.
                .requestMatchers(HttpMethod.POST, "/api/bathrooms/import/**").permitAll()
                // Everything else (submitting a new bathroom, and anything
                // added later) requires a logged-in user.
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        // Required so the browser will actually attach the httpOnly auth
        // cookie on cross-origin requests to this API (frontend on :5173,
        // backend on :8080 -- different origins even though both localhost).
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
