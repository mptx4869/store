package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.store.dto.LoginRequest;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminOrderControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    SetUpTest setUpTest;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        setUpTest.setUp();

        // Clean up
        orderRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole = roleRepository.save(adminRole);

        Role customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole = roleRepository.save(customerRole);

        // Create admin user
        User admin = User.builder()
                .username("admin2")
                .email("admin2@example.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .status("ACTIVE")
                .role(adminRole)
                .build();
        userRepository.save(admin);

        // Create customer user
        User customer = User.builder()
                .username("customer2")
                .email("customer2@example.com")
                .passwordHash(passwordEncoder.encode("customer123"))
                .status("ACTIVE")
                .role(customerRole)
                .build();
        customer = userRepository.save(customer);

        // Get tokens
        adminToken = getTokenForUser("admin2", "admin123");
        customerToken = getTokenForUser("customer2", "customer123");

        jdbcTemplate.update(
            "INSERT INTO orders (user_id, status, currency, total_amount, placed_at, shipping_address, shipping_phone, created_at, updated_at) " +
            "VALUES (?, 'DELIVERED', 'USD', 100.50, ?, '123 Main St', '555-0100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            customer.getId(), LocalDateTime.now().minusDays(2)
        );

        jdbcTemplate.update(
            "INSERT INTO orders (user_id, status, currency, total_amount, placed_at, shipping_address, shipping_phone, created_at, updated_at) " +
            "VALUES (?, 'DELIVERED', 'USD', 50.00, ?, '123 Main St', '555-0100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            customer.getId(), LocalDateTime.now().minusDays(1)
        );
    }

    private String getTokenForUser(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<Map> response = restTemplate.postForEntity("/login", loginRequest, Map.class);
        return response.getBody().get("token").toString();
    }

    @Test
    void shouldGetRevenueAnalyticsAsAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        RequestEntity<Void> request = RequestEntity
                .get("/admin/orders/analytics/revenue?startDate=" + startDate + "&endDate=" + endDate + "&groupBy=DAY")
                .headers(headers)
                .build();

        ResponseEntity<List> response = restTemplate.exchange(request, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    void shouldDenyCustomerAccessToRevenueAnalytics() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);

        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        RequestEntity<Void> request = RequestEntity
                .get("/admin/orders/analytics/revenue?startDate=" + startDate + "&endDate=" + endDate + "&groupBy=DAY")
                .headers(headers)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
