package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.annotation.DirtiesContext;

import com.example.store.dto.LoginRequest;
import com.example.store.dto.UserRoleUpdateRequest;
import com.example.store.dto.UserStatusUpdateRequest;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminUserControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    SetUpTest setUpTest;

    private String adminToken;
    private String customerToken;
    private Long testUserId;
    private Long adminUserId;

    @BeforeEach
    void setUp() {
        setUpTest.setUp();
        
        // Clean up
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
            .username("admin")
            .email("admin@example.com")
            .passwordHash(passwordEncoder.encode("admin123"))
            .status("ACTIVE")
            .role(adminRole)
            .build();
        admin = userRepository.save(admin);
        adminUserId = admin.getId();

        // Create customer user
        User customer = User.builder()
            .username("customer")
            .email("customer@example.com")
            .passwordHash(passwordEncoder.encode("customer123"))
            .status("ACTIVE")
            .role(customerRole)
            .build();
        customer = userRepository.save(customer);

        // Create test user
        User testUser = User.builder()
            .username("testuser")
            .email("test@example.com")
            .passwordHash(passwordEncoder.encode("test123"))
            .status("ACTIVE")
            .role(customerRole)
            .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();

        // Get tokens
        adminToken = getAuthToken("admin", "admin123");
        customerToken = getAuthToken("customer", "customer123");
    }

    private String getAuthToken(String username, String password) {
        RequestEntity<LoginRequest> request = RequestEntity
            .post("/login")
            .body(new LoginRequest(username, password));
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        return (String) response.getBody().get("token");
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // ==================== GET ALL USERS TESTS ====================

    @Test
    @DirtiesContext
    void shouldGetAllUsersAsAdmin() {
        RequestEntity<Void> request = RequestEntity
            .get("/admin/users")
            .headers(createAuthHeaders(adminToken))
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("content");
        assertThat(response.getBody()).containsKey("totalElements");
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(3); // admin, customer, testuser
    }

    @Test
    @DirtiesContext
    void shouldFilterUsersByRole() {
        RequestEntity<Void> request = RequestEntity
            .get("/admin/users?role=CUSTOMER")
            .headers(createAuthHeaders(adminToken))
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(2); // customer, testuser
        
        for (Map<String, Object> user : content) {
            assertThat(user.get("role")).isEqualTo("CUSTOMER");
        }
    }

    @Test
    @DirtiesContext
    void shouldFilterUsersByStatus() {
        // First, inactivate testuser
        userRepository.findById(testUserId).ifPresent(user -> {
            user.setStatus("INACTIVE");
            userRepository.save(user);
        });

        RequestEntity<Void> request = RequestEntity
            .get("/admin/users?status=ACTIVE")
            .headers(createAuthHeaders(adminToken))
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(2); // admin, customer
        
        for (Map<String, Object> user : content) {
            assertThat(user.get("status")).isEqualTo("ACTIVE");
        }
    }

    @Test
    @DirtiesContext
    void shouldDenyAccessForCustomer() {
        RequestEntity<Void> request = RequestEntity
            .get("/admin/users")
            .headers(createAuthHeaders(customerToken))
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DirtiesContext
    void shouldDenyAccessWithoutToken() {
        RequestEntity<Void> request = RequestEntity
            .get("/admin/users")
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        // Spring Security may return 401 or 403 depending on configuration
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ==================== GET USER BY ID TESTS ====================

    @Test
    @DirtiesContext
    void shouldGetUserDetailsAsAdmin() {
        RequestEntity<Void> request = RequestEntity
            .get("/admin/users/{userId}", testUserId)
            .headers(createAuthHeaders(adminToken))
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("username", "testuser");
        assertThat(response.getBody()).containsEntry("email", "test@example.com");
        assertThat(response.getBody()).containsEntry("role", "CUSTOMER");
        assertThat(response.getBody()).containsEntry("status", "ACTIVE");
        assertThat(response.getBody()).containsKey("totalOrders");
        assertThat(response.getBody()).containsKey("completedOrders");
        assertThat(response.getBody()).containsKey("totalSpent");
    }

    @Test
    @DirtiesContext
    void shouldReturn404ForNonExistentUser() {
        RequestEntity<Void> request = RequestEntity
            .get("/admin/users/{userId}", 9999L)
            .headers(createAuthHeaders(adminToken))
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "User not found");
    }

    // ==================== UPDATE USER STATUS TESTS ====================

    @Test
    @DirtiesContext
    void shouldUpdateUserStatusToInactive() {
        UserStatusUpdateRequest updateRequest = new UserStatusUpdateRequest("INACTIVE");
        
        RequestEntity<UserStatusUpdateRequest> request = RequestEntity
            .patch("/admin/users/{userId}/status", testUserId)
            .headers(createAuthHeaders(adminToken))
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "INACTIVE");
        
        // Verify in database
        User user = userRepository.findById(testUserId).orElseThrow();
        assertThat(user.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DirtiesContext
    void shouldUpdateUserStatusToActive() {
        // First set to inactive
        User user = userRepository.findById(testUserId).orElseThrow();
        user.setStatus("INACTIVE");
        userRepository.save(user);

        UserStatusUpdateRequest updateRequest = new UserStatusUpdateRequest("ACTIVE");
        
        RequestEntity<UserStatusUpdateRequest> request = RequestEntity
            .patch("/admin/users/{userId}/status", testUserId)
            .headers(createAuthHeaders(adminToken))
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ACTIVE");
        
        // Verify in database
        User updatedUser = userRepository.findById(testUserId).orElseThrow();
        assertThat(updatedUser.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DirtiesContext
    void shouldRejectInvalidStatus() {
        HttpHeaders headers = createAuthHeaders(adminToken);
        headers.set("Content-Type", "application/json");
        
        RequestEntity<String> request = RequestEntity
            .patch("/admin/users/{userId}/status", testUserId)
            .headers(headers)
            .body("{\"status\": \"INVALID_STATUS\"}");
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ==================== UPDATE USER ROLE TESTS ====================

    @Test
    @DirtiesContext
    void shouldUpdateUserRoleToAdmin() {
        UserRoleUpdateRequest updateRequest = new UserRoleUpdateRequest("ADMIN");
        
        RequestEntity<UserRoleUpdateRequest> request = RequestEntity
            .patch("/admin/users/{userId}/role", testUserId)
            .headers(createAuthHeaders(adminToken))
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("role", "ADMIN");
        
        // Verify in database
        User user = userRepository.findById(testUserId).orElseThrow();
        assertThat(user.getRole().getName()).isEqualTo("ADMIN");
    }

    @Test
    @DirtiesContext
    void shouldUpdateUserRoleToCustomer() {
        // First change to admin
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        User user = userRepository.findById(testUserId).orElseThrow();
        user.setRole(adminRole);
        userRepository.save(user);

        UserRoleUpdateRequest updateRequest = new UserRoleUpdateRequest("CUSTOMER");
        
        RequestEntity<UserRoleUpdateRequest> request = RequestEntity
            .patch("/admin/users/{userId}/role", testUserId)
            .headers(createAuthHeaders(adminToken))
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("role", "CUSTOMER");
        
        // Verify in database
        User updatedUser = userRepository.findById(testUserId).orElseThrow();
        assertThat(updatedUser.getRole().getName()).isEqualTo("CUSTOMER");
    }

    @Test
    @DirtiesContext
    void shouldRejectInvalidRole() {
        HttpHeaders headers = createAuthHeaders(adminToken);
        headers.set("Content-Type", "application/json");
        
        RequestEntity<String> request = RequestEntity
            .patch("/admin/users/{userId}/role", testUserId)
            .headers(headers)
            .body("{\"role\": \"SUPERUSER\"}");
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ==================== ADDITIONAL SECURITY TESTS ====================

    @Test
    @DirtiesContext
    void shouldDenyCustomerAccessToUserDetails() {
        RequestEntity<Void> request = RequestEntity
            .get("/admin/users/{userId}", testUserId)
            .headers(createAuthHeaders(customerToken))
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DirtiesContext
    void shouldDenyCustomerFromUpdatingStatus() {
        UserStatusUpdateRequest updateRequest = new UserStatusUpdateRequest("INACTIVE");
        
        RequestEntity<UserStatusUpdateRequest> request = RequestEntity
            .patch("/admin/users/{userId}/status", testUserId)
            .headers(createAuthHeaders(customerToken))
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DirtiesContext
    void shouldDenyCustomerFromUpdatingRole() {
        UserRoleUpdateRequest updateRequest = new UserRoleUpdateRequest("ADMIN");
        
        RequestEntity<UserRoleUpdateRequest> request = RequestEntity
            .patch("/admin/users/{userId}/role", testUserId)
            .headers(createAuthHeaders(customerToken))
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DirtiesContext
    void shouldAllowAdminToUpdateThemselves() {
        UserStatusUpdateRequest updateRequest = new UserStatusUpdateRequest("ACTIVE");
        
        RequestEntity<UserStatusUpdateRequest> request = RequestEntity
            .patch("/admin/users/{userId}/status", adminUserId)
            .headers(createAuthHeaders(adminToken))
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ACTIVE");
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    @DirtiesContext
    void shouldReturnPaginatedUsers() {
        // Create more users for pagination test
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
        for (int i = 1; i <= 10; i++) {
            User user = User.builder()
                .username("user" + i)
                .email("user" + i + "@example.com")
                .passwordHash(passwordEncoder.encode("pass123"))
                .status("ACTIVE")
                .role(customerRole)
                .build();
            userRepository.save(user);
        }

        RequestEntity<Void> request = RequestEntity
            .get("/admin/users?size=5&page=0")
            .headers(createAuthHeaders(adminToken))
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(5);
        assertThat(response.getBody().get("totalElements")).isEqualTo(13); // 3 original + 10 new
        assertThat(response.getBody().get("totalPages")).isEqualTo(3);
    }
}
