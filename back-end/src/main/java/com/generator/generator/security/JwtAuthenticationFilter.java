package com.generator.generator.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip filter for permitted endpoints
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/api/auth/") || 
            requestPath.startsWith("/swagger-ui/") || 
            requestPath.startsWith("/v3/api-docs/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // For SSE endpoints, we need to validate authentication BEFORE response is committed
        boolean isSseEndpoint = requestPath.contains("/stream");
        
        String token = getTokenFromRequest(request);

        if (token != null && tokenProvider.validateToken(token)) {
            try {
                String username = tokenProvider.getUsernameFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Successfully authenticated user: {} for path: {}", username, requestPath);
            } catch (Exception e) {
                log.warn("Failed to authenticate user with token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                
                        // For SSE endpoints, don't fail here - let the controller handle it
                        // because SSE responses need special handling
                        // The controller will check authentication before creating emitter
                        log.debug("Authentication failed for SSE endpoint, will be handled by controller");
            }
        } else if (token != null) {
            log.warn("Invalid or expired token received for path: {}", requestPath);
            SecurityContextHolder.clearContext();
            
            // For SSE endpoints, let Spring Security handle authorization
            // The controller will validate authentication before creating emitter
            log.debug("Invalid token for SSE endpoint, will be handled by authorization filter");
        } else {
            // No token provided - for SSE endpoints that require auth, fail early
            if (isSseEndpoint && requestPath.contains("/api/projects/") && requestPath.contains("/generate/")) {
                log.warn("No token provided for SSE endpoint: {}", requestPath);
                // Don't fail here - let Spring Security handle it, but log it
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        // First try to get from Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Then try to get from query parameter (for SSE/EventSource)
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        // Then try to get from cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> "token".equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }

        return null;
    }
}

