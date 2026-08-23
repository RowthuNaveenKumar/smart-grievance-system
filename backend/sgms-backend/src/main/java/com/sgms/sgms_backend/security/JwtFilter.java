package com.sgms.sgms_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            try {
                Claims claims = jwtUtil.extractClaims(token);

                String email = claims.getSubject();

                String role = claims.get("role", String.class);
                String subRole = claims.get("subRole", String.class);

                List<SimpleGrantedAuthority> authorities =
                        new java.util.ArrayList<>();

                authorities.add(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                if (subRole != null) {
                    authorities.add(
                            new SimpleGrantedAuthority("ROLE_" + subRole)
                    );
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException ex) {
                // Token is expired, invalid signature, or malformed.
                // Log it and continue — Spring Security will reject the request
                // with 403 (unauthenticated) rather than a 500 server error.
                log.warn("Invalid JWT token: {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("JWT processing error: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}