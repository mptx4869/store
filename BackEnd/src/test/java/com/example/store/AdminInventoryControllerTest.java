package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

import com.example.store.dto.InventoryListResponse;
import com.example.store.dto.InventoryUpdateRequest;
import com.example.store.dto.LoginRequest;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminInventoryControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SetUpTest setUpTest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String customerToken;
    private final Long testSkuId = 100L; // From SQL script

    @BeforeEach
    void setUp() {
        // Clean database first
        setUpTest.setUp();

        // Create roles using Java
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator role");
        roleRepository.save(adminRole);

        Role customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole.setDescription("Customer role");
        roleRepository.save(customerRole);

        // Create users using Java (for proper password hashing)
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(adminRole);
        admin.setStatus("ACTIVE");
        userRepository.save(admin);

        User customer = new User();
        customer.setUsername("customer");
        customer.setEmail("customer@test.com");
        customer.setPasswordHash(passwordEncoder.encode("customer123"));
        customer.setRole(customerRole);
        customer.setStatus("ACTIVE");
        userRepository.save(customer);

        // Create book, SKU, and inventory using JDBC to avoid @MapsId issues
        jdbcTemplate.update(
            "INSERT INTO books (id, title, base_price, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            100, "Test Book", 29.99
        );
        
        jdbcTemplate.update(
            "INSERT INTO product_skus (id, book_id, sku, format, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            100, 100, "TEST-SKU-001", "PAPERBACK"
        );
        
        jdbcTemplate.update(
            "UPDATE books SET default_sku_id = ? WHERE id = ?",
            100, 100
        );
        
        jdbcTemplate.update(
            "INSERT INTO inventory (sku_id, stock, reserved, last_updated) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
            100, 5, 0
        );
        
        // Login with created users
        adminToken = login("admin", "admin123");
        customerToken = login("customer", "customer123");
    }

    @Test
    void shouldGetAllInventoryAsAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            "/admin/inventory?page=0&size=20",
            HttpMethod.GET,
            request,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isInstanceOf(List.class);
        
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isNotEmpty();
    }

    @Test
    void shouldGetLowStockItems() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<InventoryListResponse[]> response = restTemplate.exchange(
            "/admin/inventory/low-stock",
            HttpMethod.GET,
            request,
            InventoryListResponse[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
        
        // Verify first item is low stock
        InventoryListResponse item = response.getBody()[0];
        assertThat(item.availableStock()).isLessThanOrEqualTo(10);
        assertThat(item.status()).isIn("LOW_STOCK", "OUT_OF_STOCK");
    }

    @Test
    void shouldAddStockSuccessfully() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.add("Content-Type", "application/json");

        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest(
            50,  // Add 50 items
            null,
            "ADD"
        );

        HttpEntity<InventoryUpdateRequest> request = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<InventoryListResponse> response = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request,
            InventoryListResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalStock()).isEqualTo(55); // 5 + 50
        assertThat(response.getBody().availableStock()).isEqualTo(55);
        assertThat(response.getBody().status()).isEqualTo("IN_STOCK");
    }

    @Test
    void shouldSetStockSuccessfully() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.add("Content-Type", "application/json");

        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest(
            100,  // Set to exactly 100
            null,
            "SET"
        );

        HttpEntity<InventoryUpdateRequest> request = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<InventoryListResponse> response = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request,
            InventoryListResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalStock()).isEqualTo(100);
        assertThat(response.getBody().status()).isEqualTo("IN_STOCK");
    }

    @Test
    void shouldNotAllowSetStockBelowReserved() {
        // Reserve some stock using SQL
        jdbcTemplate.update("UPDATE inventory SET reserved = 3 WHERE sku_id = ?", testSkuId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.add("Content-Type", "application/json");

        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest(
            2,  // Try to set stock to 2 (but reserved is 3)
            null,
            "SET"
        );

        HttpEntity<InventoryUpdateRequest> request = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).asString()
            .contains("Cannot set stock to 2")
            .contains("Reserved stock is 3");
    }

    @Test
    void shouldReturn404ForNonExistentSku() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.add("Content-Type", "application/json");

        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest(100, null, "SET");
        HttpEntity<InventoryUpdateRequest> request = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            "/admin/inventory/99999",
            HttpMethod.PUT,
            request,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).asString()
            .contains("Inventory not found");
    }

    @Test
    void shouldDenyAccessForCustomer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            "/admin/inventory",
            HttpMethod.GET,
            request,
            Map.class
        );

        // Customer role must be denied access to admin endpoints
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldDenyAccessWithoutToken() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/admin/inventory",
            Map.class
        );

        // Spring Security returns 403 for missing authentication
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldRejectInvalidAction() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.add("Content-Type", "application/json");

        // Create request with invalid action
        String jsonRequest = "{\"stock\": 100, \"action\": \"INVALID\"}";
        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request,
            Map.class
        );

        // DTO validation throws exception, resulting in 500 or 400
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldRejectNegativeStock() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.add("Content-Type", "application/json");

        // Create request with negative stock
        String jsonRequest = "{\"stock\": -10, \"action\": \"SET\"}";
        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request,
            Map.class
        );

        // DTO validation throws exception, resulting in 500 or 400
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldDefaultToSetActionWhenNotSpecified() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.add("Content-Type", "application/json");

        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest(
            25,
            null,
            null  // No action specified
        );

        HttpEntity<InventoryUpdateRequest> request = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<InventoryListResponse> response = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request,
            InventoryListResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalStock()).isEqualTo(25); // SET to 25, not ADD
    }

    @Test
    void shouldShowCorrectStatusAfterUpdate() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.add("Content-Type", "application/json");

        // Set to 0 - should be OUT_OF_STOCK
        InventoryUpdateRequest updateRequest1 = new InventoryUpdateRequest(0, null, "SET");
        HttpEntity<InventoryUpdateRequest> request1 = new HttpEntity<>(updateRequest1, headers);
        
        ResponseEntity<InventoryListResponse> response1 = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request1,
            InventoryListResponse.class
        );

        assertThat(response1.getBody().status()).isEqualTo("OUT_OF_STOCK");

        // Set to 5 - should be LOW_STOCK
        InventoryUpdateRequest updateRequest2 = new InventoryUpdateRequest(5, null, "SET");
        HttpEntity<InventoryUpdateRequest> request2 = new HttpEntity<>(updateRequest2, headers);
        
        ResponseEntity<InventoryListResponse> response2 = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request2,
            InventoryListResponse.class
        );

        assertThat(response2.getBody().status()).isEqualTo("LOW_STOCK");

        // Set to 100 - should be IN_STOCK
        InventoryUpdateRequest updateRequest3 = new InventoryUpdateRequest(100, null, "SET");
        HttpEntity<InventoryUpdateRequest> request3 = new HttpEntity<>(updateRequest3, headers);
        
        ResponseEntity<InventoryListResponse> response3 = restTemplate.exchange(
            "/admin/inventory/" + testSkuId,
            HttpMethod.PUT,
            request3,
            InventoryListResponse.class
        );

        assertThat(response3.getBody().status()).isEqualTo("IN_STOCK");
    }

    private String login(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);
        ResponseEntity<Map> loginResponse = restTemplate
            .postForEntity("/login", loginRequest, Map.class);
        
        if (loginResponse.getBody() == null) {
            throw new RuntimeException("Login failed for " + username + 
                ": status=" + loginResponse.getStatusCode());
        }
        
        return (String) loginResponse.getBody().get("token");
    }
}
