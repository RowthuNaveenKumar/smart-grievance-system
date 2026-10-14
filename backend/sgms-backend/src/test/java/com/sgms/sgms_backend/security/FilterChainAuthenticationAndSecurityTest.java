package com.sgms.sgms_backend.security;

import com.sgms.sgms_backend.service.DesignationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Phase 2.2: Filter Chain HTTP 401 vs 403 Security Behavior Tests")
class FilterChainAuthenticationAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @MockBean
    private DesignationService designationService;

    // =========================================================================
    // 1. UNAUTHENTICATED REQUESTS -> HTTP 401 UNAUTHORIZED
    // =========================================================================

    @Test
    @DisplayName("FC1: Missing JWT returns HTTP 401 Unauthorized with standard ErrorResponse JSON")
    void missingJwt_returnsHttp401() throws Exception {
        mockMvc.perform(get("/admin/designations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("FC2: Malformed JWT string returns HTTP 401 Unauthorized")
    void malformedJwt_returnsHttp401() throws Exception {
        mockMvc.perform(get("/admin/designations")
                        .header("Authorization", "Bearer not.a.valid.jwt.structure")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("FC3: Tampered signature JWT returns HTTP 401 Unauthorized")
    void tamperedJwt_returnsHttp401() throws Exception {
        String validToken = jwtUtil.generateToken("admin@sgms.com", "STAFF", "ADMIN", "STAFF");
        // Tamper with the signature bytes at the end
        String tamperedToken = validToken.substring(0, validToken.length() - 6) + "XXXXXX";

        mockMvc.perform(get("/admin/designations")
                        .header("Authorization", "Bearer " + tamperedToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("FC4: Expired JWT returns HTTP 401 Unauthorized")
    void expiredJwt_returnsHttp401() throws Exception {
        // Construct a token that expired 10 seconds ago
        String expiredToken = Jwts.builder()
                .setSubject("admin@sgms.com")
                .claim("role", "STAFF")
                .claim("subRole", "ADMIN")
                .claim("userType", "STAFF")
                .setIssuedAt(new Date(System.currentTimeMillis() - 30000))
                .setExpiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/admin/designations")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    // =========================================================================
    // 2. AUTHENTICATED UNAUTHORIZED REQUESTS -> HTTP 403 FORBIDDEN
    // =========================================================================

    @Test
    @DisplayName("FC5: Authenticated non-admin STUDENT receives HTTP 403 Forbidden accessing /admin/designations")
    void authenticatedStudent_accessingAdminEndpoints_returnsHttp403() throws Exception {
        String studentToken = jwtUtil.generateToken("student@sgms.com", "STUDENT", null, "STUDENT");

        mockMvc.perform(get("/admin/designations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("FC6: Authenticated ordinary STAFF (e.g. WARDEN) receives HTTP 403 Forbidden accessing /admin/designations")
    void authenticatedStaff_accessingAdminEndpoints_returnsHttp403() throws Exception {
        String staffToken = jwtUtil.generateToken("warden@sgms.com", "STAFF", "WARDEN", "STAFF");

        mockMvc.perform(get("/admin/designations")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));

        // Also check POST and DELETE
        mockMvc.perform(post("/admin/designations")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TEST\",\"assignmentScope\":\"GLOBAL\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/admin/designations/5")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. AUTHENTICATED PLATFORM ADMIN -> HTTP 200 OK
    // =========================================================================

    @Test
    @DisplayName("FC7: Authenticated genuine platform admin receives HTTP 200 OK accessing /admin/designations")
    void authenticatedAdmin_accessingAdminEndpoints_returnsHttp200() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@sgms.com", "STAFF", "ADMIN", "STAFF");

        when(designationService.getAllDesignations()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/designations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
