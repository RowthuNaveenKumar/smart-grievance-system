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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sgms.sgms_backend.repository.UserRepository;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepo) {
        this.jwtUtil = jwtUtil;
        this.userRepo = userRepo;
    }

    public JwtFilter(JwtUtil jwtUtil) {
        this(jwtUtil, null);
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

                // Real-time account status verification:
                // Immediately reject requests from disabled accounts so previously issued
                // JWTs cannot be used after deactivation.
                if (userRepo != null && email != null) {
                    var userOpt = userRepo.findByEmail(email);
                    if (userOpt.isPresent() && !userOpt.get().isEnabled()) {
                        log.warn("Access rejected for disabled account: {}", email);
                        filterChain.doFilter(request, response);
                        return;
                    }
                }

                String role = claims.get("role", String.class);
                String subRole = claims.get("subRole", String.class);

                List<SimpleGrantedAuthority> authorities =
                        new java.util.ArrayList<>();

                if (role != null) {
                    authorities.add(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );
                }

                // Platform administrator authority:
                // Granted when the verified token carries the ADMIN role/subRole.
                // Dynamic workflow designations (e.g. WARDEN, HOD, DEAN, PROCTOR)
                // are business workflow roles and must NOT be granted as Spring Security authorities.
                if ("ADMIN".equalsIgnoreCase(subRole) || "ADMIN".equalsIgnoreCase(role)) {
                    if (!"ADMIN".equalsIgnoreCase(role)) {
                        authorities.add(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        );
                    }
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