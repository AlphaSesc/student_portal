package com.example.student_portal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
// Filter that runs once per request to validate JWT and authenticate user
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Step 1: Get Authorization header from request
        final String authHeader = request.getHeader("Authorization");

        // If no token or invalid format, skip authentication and continue request
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract JWT token (remove "Bearer ")
        final String jwt = authHeader.substring(7);
        final String email;

        try {
            // Step 3: Extract user email from token
            email = jwtService.extractUsername(jwt);
        } catch (Exception ex) {
            // If token is invalid, skip authentication
            filterChain.doFilter(request, response);
            return;
        }

        // Step 4: Check if user is not already authenticated
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load user details from database
            CustomUserDetails userDetails =
                    (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);

            // Step 5: Validate token against user details
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Create authentication object with user info and roles
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Attach request details (IP, session info)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Store authentication in SecurityContext (user is now authenticated)
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Continue processing request
        filterChain.doFilter(request, response);
    }
}