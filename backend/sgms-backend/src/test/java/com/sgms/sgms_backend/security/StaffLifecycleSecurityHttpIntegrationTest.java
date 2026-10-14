package com.sgms.sgms_backend.security;

import com.sgms.sgms_backend.dto.StatusResponse;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.service.AdminManagementService;
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
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Phase 2.4A: Staff Lifecycle HTTP Authorization & Security Integration Tests")
class StaffLifecycleSecurityHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @MockBean
    private AdminManagementService adminManagementService;

    // =========================================================================
    // 1. UNAUTHENTICATED REQUESTS -> HTTP 401 UNAUTHORIZED
    // =========================================================================

    @Test
    @DisplayName("SL1: Missing JWT token returns HTTP 401 Unauthorized")
    void missingToken_enableStaff_returnsHttp401() throws Exception {
        mockMvc.perform(patch("/admin/staff/2/enable")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("SL2: Malformed JWT token returns HTTP 401 Unauthorized")
    void malformedToken_disableStaff_returnsHttp401() throws Exception {
        mockMvc.perform(patch("/admin/staff/2/disable")
                        .header("Authorization", "Bearer invalid.malformed.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("SL3: Tampered signature JWT returns HTTP 401 Unauthorized")
    void tamperedToken_enableStaff_returnsHttp401() throws Exception {
        String validAdminToken = jwtUtil.generateToken("admin@sgms.com", "STAFF", "ADMIN", "STAFF");
        String tamperedToken = validAdminToken.substring(0, validAdminToken.length() - 6) + "TAMPER";

        mockMvc.perform(patch("/admin/staff/2/enable")
                        .header("Authorization", "Bearer " + tamperedToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("SL4: Expired JWT returns HTTP 401 Unauthorized")
    void expiredToken_disableStaff_returnsHttp401() throws Exception {
        String expiredToken = Jwts.builder()
                .setSubject("admin@sgms.com")
                .claim("role", "STAFF")
                .claim("subRole", "ADMIN")
                .claim("userType", "STAFF")
                .setIssuedAt(new Date(System.currentTimeMillis() - 60000))
                .setExpiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(patch("/admin/staff/2/disable")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    // =========================================================================
    // 2. NON-ADMIN AUTHENTICATED REQUESTS -> HTTP 403 FORBIDDEN
    // =========================================================================

    @Test
    @DisplayName("SL5: Student token calling PATCH /admin/staff/{id}/enable returns HTTP 403 Forbidden")
    void studentToken_enableStaff_returnsHttp403() throws Exception {
        String studentToken = jwtUtil.generateToken("student@sgms.com", "STUDENT", null, "STUDENT");

        mockMvc.perform(patch("/admin/staff/2/enable")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("SL6: Student token calling PATCH /admin/staff/{id}/disable returns HTTP 403 Forbidden")
    void studentToken_disableStaff_returnsHttp403() throws Exception {
        String studentToken = jwtUtil.generateToken("student@sgms.com", "STUDENT", null, "STUDENT");

        mockMvc.perform(patch("/admin/staff/2/disable")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("SL7: Ordinary staff (WARDEN) token calling PATCH /admin/staff/{id}/enable returns HTTP 403 Forbidden")
    void wardenToken_enableStaff_returnsHttp403() throws Exception {
        String wardenToken = jwtUtil.generateToken("warden@sgms.com", "STAFF", "WARDEN", "STAFF");

        mockMvc.perform(patch("/admin/staff/2/enable")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("SL8: Departmental staff (HOD) token calling PATCH /admin/staff/{id}/disable returns HTTP 403 Forbidden")
    void hodToken_disableStaff_returnsHttp403() throws Exception {
        String hodToken = jwtUtil.generateToken("hod@sgms.com", "STAFF", "HOD", "STAFF");

        mockMvc.perform(patch("/admin/staff/2/disable")
                        .header("Authorization", "Bearer " + hodToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    // =========================================================================
    // 3. AUTHORIZED PLATFORM ADMINISTRATOR -> HTTP 200 OK
    // =========================================================================

    @Test
    @DisplayName("SL9: Platform administrator successfully enables staff account (HTTP 200 OK)")
    void adminToken_enableStaff_returnsHttp200() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@sgms.com", "STAFF", "ADMIN", "STAFF");

        when(adminManagementService.enableStaff(2L))
                .thenReturn(new StatusResponse("Staff enabled successfully"));

        mockMvc.perform(patch("/admin/staff/2/enable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Staff enabled successfully")));
    }

    @Test
    @DisplayName("SL10: Platform administrator successfully disables staff account (HTTP 200 OK)")
    void adminToken_disableStaff_returnsHttp200() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@sgms.com", "STAFF", "ADMIN", "STAFF");

        when(adminManagementService.disableStaff(2L))
                .thenReturn(new StatusResponse("Staff disabled successfully"));

        mockMvc.perform(patch("/admin/staff/2/disable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Staff disabled successfully")));
    }

    // =========================================================================
    // 4. LAST-ACTIVE ADMINISTRATOR DEACTIVATION -> HTTP 400 BAD REQUEST
    // =========================================================================

    @Test
    @DisplayName("SL11: Disabling last active platform administrator returns HTTP 400 Bad Request with documented error")
    void adminToken_disableLastActiveAdmin_returnsHttp400() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@sgms.com", "STAFF", "ADMIN", "STAFF");

        when(adminManagementService.disableStaff(1L))
                .thenThrow(new ValidationException("Cannot disable the last active platform administrator account"));

        mockMvc.perform(patch("/admin/staff/1/disable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Cannot disable the last active platform administrator account")));
    }
}
