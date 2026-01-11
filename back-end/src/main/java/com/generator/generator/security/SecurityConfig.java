package com.generator.generator.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                // Handle exceptions gracefully for SSE endpoints
                // If response is already committed (SSE started), don't try to modify it
                .authenticationEntryPoint((request, response, authException) -> {
                    // Check if response is already committed (SSE streams commit early)
                    if (response.isCommitted()) {
                        // Response already sent, can't modify it - controller handles errors
                        return;
                    }
                    String path = request.getRequestURI();
                    if (path != null && path.contains("/stream")) {
                        // For SSE endpoints, controller validates auth before creating emitter
                        // If we reach here, something went wrong but response not committed yet
                        return; // Let controller handle it
                    }
                    // For non-SSE endpoints, send error response
                    try {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Unauthorized\"}");
                    } catch (Exception e) {
                        // Response might have been committed in the meantime
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // Check if response is already committed (SSE streams commit early)
                    if (response.isCommitted()) {
                        // Response already sent, can't modify it - controller handles errors
                        return;
                    }
                    String path = request.getRequestURI();
                    if (path != null && path.contains("/stream")) {
                        // For SSE endpoints, controller validates auth before creating emitter
                        // If we reach here, something went wrong but response not committed yet
                        return; // Let controller handle it
                    }
                    // For non-SSE endpoints, send error response
                    try {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Access Denied\"}");
                    } catch (Exception e) {
                        // Response might have been committed in the meantime
                    }
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                // SSE endpoints - allow any authenticated user (use custom authorization manager)
                .requestMatchers("/api/projects/*/generate/backend/stream").access(authenticatedAuthorizationManager())
                .requestMatchers("/api/projects/*/generate/frontend/stream").access(authenticatedAuthorizationManager())
                .requestMatchers("/api/generate/backend/stream").access(authenticatedAuthorizationManager())
                .requestMatchers("/api/generate/frontend/stream").access(authenticatedAuthorizationManager())
                .requestMatchers("/api/project/**").authenticated()
                .requestMatchers("/api/templates/**").authenticated()
                .anyRequest().authenticated()
            )
            .userDetailsService(userDetailsService)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Custom authorization manager that allows any authenticated user.
     * This ensures that Spring Security 7.x AuthorizationFilter allows authenticated users
     * even if they don't have specific authorities.
     */
    private AuthorizationManager<RequestAuthorizationContext> authenticatedAuthorizationManager() {
        return (authentication, context) -> {
            if (authentication == null || !authentication.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            // Allow any authenticated user
            return new AuthorizationDecision(true);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow localhost for development and VPS IP for production
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:4200",
            "http://localhost:8080",
            "http://102.211.210.197",
            "http://102.211.210.197:80",
            "http://102.211.210.197:4200",
            "http://102.211.210.197:8080",
            "http://102.211.210.197:8090",
            "http://localhost:8090"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control", "X-Accel-Buffering"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

