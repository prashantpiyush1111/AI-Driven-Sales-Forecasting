package com.salesforecast.integration;

import com.jayway.jsonpath.JsonPath;
import com.salesforecast.dto.ml.MlPredictResponse;
import com.salesforecast.service.MLClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test against a real MySQL (Testcontainers): Flyway migrations, Hibernate schema
 * validation, security, and the main API flows. The ML service is mocked.
 * Skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "bootstrap.admin.password=Admin12345",
        "spring.profiles.active=dev"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MLClientService mlClientService;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @Order(1)
    @DisplayName("app starts: Flyway migrated and Hibernate schema validation passed; health is UP")
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(2)
    @DisplayName("admin bootstrapped from ADMIN_PASSWORD can log in; /me returns ADMIN")
    void adminLoginAndProfile() throws Exception {
        String token = login("admin", "Admin12345");

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @Order(3)
    @DisplayName("register -> 201 with token; duplicate -> 409; wrong password login -> 401")
    void registerFlow() throws Exception {
        String body = "{\"username\":\"itUser\",\"email\":\"it@example.com\",\"password\":\"Passw0rd123\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"itUser\",\"password\":\"wrong-password1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("seeded sales data is queryable: list, summary, filters")
    void salesReadQueries() throws Exception {
        String token = login("admin", "Admin12345");

        mockMvc.perform(get("/api/sales").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6));
        mockMvc.perform(get("/api/sales/summary").param("productName", "Laptop Pro 15")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(43))
                .andExpect(jsonPath("$.recordCount").value(2));
        mockMvc.perform(get("/api/sales/filters").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(4)));
    }

    @Test
    @Order(5)
    @DisplayName("non-admin cannot write sales (403); admin can (201)")
    void salesWriteAuthorization() throws Exception {
        String body = """
                {"productName":"Keyboard","category":"Accessories","region":"East",
                 "quantity":10,"revenue":5000.00,"saleDate":"2026-02-01"}
                """;
        String userToken = JsonPath.read(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"reader\",\"email\":\"reader@example.com\",\"password\":\"Passw0rd123\"}"))
                .andReturn().getResponse().getContentAsString(), "$.token");
        String adminToken = login("admin", "Admin12345");

        mockMvc.perform(post("/api/sales").header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/sales").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Keyboard"));
    }

    @Test
    @Order(6)
    @DisplayName("analytics queries run on real MySQL")
    void analytics() throws Exception {
        String token = login("admin", "Admin12345");

        mockMvc.perform(get("/api/analytics/overview").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordCount").isNumber());
        mockMvc.perform(get("/api/analytics/trend").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].period").value("2026-01"));
        mockMvc.perform(get("/api/analytics/top-products").param("limit", "2")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Laptop Pro 15"));
        mockMvc.perform(get("/api/analytics/by-category").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/by-region").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    @DisplayName("forecast: generate (ML mocked) -> stored -> listed; unknown product -> 400")
    void forecastFlow() throws Exception {
        when(mlClientService.predict(any(), any())).thenReturn(
                new MlPredictResponse(42, new BigDecimal("2100000.00"), new BigDecimal("0.8734"), "lgbm-v1"));
        String token = login("admin", "Admin12345");
        String date = LocalDate.now().plusDays(30).toString();

        mockMvc.perform(post("/api/forecasts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Laptop Pro 15\",\"forecastDate\":\"" + date + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.predictedQuantity").value(42));

        mockMvc.perform(get("/api/forecasts/Laptop Pro 15").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/forecasts/Laptop Pro 15/latest").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelVersion").value("lgbm-v1"));
        mockMvc.perform(get("/api/forecasts/Laptop Pro 15")
                        .param("from", LocalDate.now().plusDays(60).toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/forecasts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Ghost Product\",\"forecastDate\":\"" + date + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(8)
    @DisplayName("admin can list users and promote one; USER cannot")
    void adminUsers() throws Exception {
        String adminToken = login("admin", "Admin12345");
        String userToken = login("itUser", "Passw0rd123");

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        String list = mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<Integer> readerIds = JsonPath.read(list, "$.content[?(@.username=='reader')].id");
        Integer reader = readerIds.get(0);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/users/" + reader + "/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
