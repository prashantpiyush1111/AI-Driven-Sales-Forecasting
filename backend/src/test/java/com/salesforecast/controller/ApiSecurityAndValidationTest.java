package com.salesforecast.controller;

import com.salesforecast.config.SecurityConfig;
import com.salesforecast.dto.response.AuthResponse;
import com.salesforecast.dto.response.ForecastResponse;
import com.salesforecast.dto.response.SalesRecordResponse;
import com.salesforecast.dto.response.UserResponse;
import com.salesforecast.security.JwtUtil;
import com.salesforecast.security.UserDetailsServiceImpl;
import com.salesforecast.service.AnalyticsService;
import com.salesforecast.service.AuthService;
import com.salesforecast.service.ForecastService;
import com.salesforecast.service.SalesRecordService;
import com.salesforecast.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the web + security layers: authentication, role checks and request validation.
 * Services are mocked; no database or ML service is needed.
 */
@WebMvcTest({
        AuthController.class,
        SalesRecordController.class,
        ForecastController.class,
        AdminUserController.class,
        AnalyticsController.class
})
@Import(SecurityConfig.class)
class ApiSecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private SalesRecordService salesRecordService;
    @MockBean
    private ForecastService forecastService;
    @MockBean
    private UserService userService;
    @MockBean
    private AnalyticsService analyticsService;

    // Needed by SecurityConfig / JwtAuthFilter
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private static final String SALES_BODY = """
            {"productName":"Laptop Pro 15","category":"Electronics","region":"North",
             "quantity":5,"revenue":4999.50,"saleDate":"2026-01-15"}
            """;

    // ---------- authentication ----------

    @Test
    @DisplayName("protected endpoint without token -> 401")
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/sales")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analytics/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login is public and returns the token")
    void login_isPublic() throws Exception {
        when(authService.login(any())).thenReturn(AuthResponse.builder()
                .token("jwt").userId(1L).username("admin").role("ADMIN").build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("/api/auth/me returns the current user's profile")
    void me_returnsProfile() throws Exception {
        when(authService.getProfile("admin")).thenReturn(
                new UserResponse(1L, "admin", "admin@salesforecast.com", "ADMIN", null));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ---------- registration validation ----------

    @Test
    @DisplayName("register: short password -> 400 with field error")
    void register_shortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"prashant\",\"email\":\"p@example.com\",\"password\":\"abc12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("register: password without a digit -> 400")
    void register_weakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"prashant\",\"email\":\"p@example.com\",\"password\":\"onlyletters\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // ---------- role checks on sales ----------

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("sales: USER can read")
    void sales_userCanRead() throws Exception {
        when(salesRecordService.search(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/sales")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("sales: USER cannot create, update or delete -> 403")
    void sales_userCannotWrite() throws Exception {
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(SALES_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(put403("/api/sales/1")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/sales/1")).andExpect(status().isForbidden());
        verifyNoInteractions(salesRecordService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("sales: ADMIN can create (201) and delete (204)")
    void sales_adminCanWrite() throws Exception {
        when(salesRecordService.create(any())).thenReturn(SalesRecordResponse.builder().id(9L)
                .productName("Laptop Pro 15").quantity(5).revenue(new BigDecimal("4999.50"))
                .saleDate(LocalDate.of(2026, 1, 15)).build());

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(SALES_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
        mockMvc.perform(delete("/api/sales/9")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("sales: negative quantity -> 400 with field error")
    void sales_validation() throws Exception {
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"X\",\"quantity\":-1,\"revenue\":10,\"saleDate\":\"2026-01-15\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    // ---------- forecasts ----------

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("forecast: USER can generate a forecast (201)")
    void forecast_generate() throws Exception {
        when(forecastService.generateForecast(any(), any())).thenReturn(ForecastResponse.builder()
                .id(1L).productName("Laptop Pro 15").predictedQuantity(42)
                .predictedRevenue(new BigDecimal("2100000.00")).build());

        mockMvc.perform(post("/api/forecasts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Laptop Pro 15\",\"forecastDate\":\"2099-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.predictedQuantity").value(42));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("forecast: past date -> 400")
    void forecast_pastDate() throws Exception {
        mockMvc.perform(post("/api/forecasts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Laptop Pro 15\",\"forecastDate\":\"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.forecastDate").exists());
        verifyNoInteractions(forecastService);
    }

    // ---------- admin ----------

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("admin users: USER -> 403")
    void admin_userForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("admin users: ADMIN can list; invalid role -> 400")
    void admin_adminAllowed() throws Exception {
        when(userService.list(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPERUSER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    // ---------- analytics ----------

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("analytics: invalid granularity -> 400")
    void analytics_invalidGranularity() throws Exception {
        mockMvc.perform(get("/api/analytics/trend").param("granularity", "YEARLY"))
                .andExpect(status().isBadRequest());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder put403(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SALES_BODY);
    }
}
