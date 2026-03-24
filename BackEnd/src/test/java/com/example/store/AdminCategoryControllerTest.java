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

import com.example.store.dto.AdminCategoryResponse;
import com.example.store.dto.CategoryCreateRequest;
import com.example.store.dto.CategoryUpdateRequest;
import com.example.store.dto.LoginRequest;
import com.example.store.model.Category;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.BookCategoryRepository;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCategoryControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookCategoryRepository bookCategoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    SetUpTest setUpTest;

    private String adminToken;
    private String customerToken;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        setUpTest.setUp();

        // Clean up
        bookCategoryRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator");
        roleRepository.save(adminRole);

        Role customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole.setDescription("Customer");
        roleRepository.save(customerRole);

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(adminRole);
        admin.setStatus("ACTIVE");
        userRepository.save(admin);

        // Create customer user
        User customer = new User();
        customer.setUsername("customer");
        customer.setEmail("customer@test.com");
        customer.setPasswordHash(passwordEncoder.encode("customer123"));
        customer.setRole(customerRole);
        customer.setStatus("ACTIVE");
        userRepository.save(customer);

        // Login to get tokens
        adminToken = loginAndGetToken("admin", "admin123");
        customerToken = loginAndGetToken("customer", "customer123");

        // Create test category
        Category testCategory = Category.builder()
            .name("Fiction")
            .description("Fiction books")
            .build();
        testCategory = categoryRepository.save(testCategory);
        testCategoryId = testCategory.getId();
    }

    private String loginAndGetToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        
        ResponseEntity<Map> response = restTemplate.postForEntity("/login", loginRequest, Map.class);
        
        System.out.println("Login response status: " + response.getStatusCode());
        System.out.println("Login response body: " + response.getBody());
        
        if (response.getBody() == null) {
            throw new RuntimeException("Login failed - response body is null");
        }
        
        Object token = response.getBody().get("token");
        if (token == null) {
            throw new RuntimeException("Token not found in response: " + response.getBody());
        }
        
        return token.toString();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    // ========== Authorization Tests (3) ==========

    @Test
    void shouldDenyAccessWithoutToken() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/admin/categories", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldDenyAccessForCustomer() {
        HttpHeaders headers = createAuthHeaders(customerToken);
        RequestEntity<Void> request = RequestEntity.get("/admin/categories")
            .headers(headers)
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAllowAccessForAdmin() {
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<Void> request = RequestEntity.get("/admin/categories")
            .headers(headers)
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ========== List Tests (2) ==========

    @Test
    void shouldReturnPaginatedCategories() {
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<Void> request = RequestEntity.get("/admin/categories?page=0&size=10")
            .headers(headers)
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
        assertThat(response.getBody().get("totalElements")).isNotNull();
    }

    @Test
    void shouldReturnCategoriesWithBookCount() {
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<Void> request = RequestEntity.get("/admin/categories")
            .headers(headers)
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("content")).asList().isNotEmpty();
    }

    // ========== Get Details Tests (2) ==========

    @Test
    void shouldGetCategoryDetails() {
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<Void> request = RequestEntity.get("/admin/categories/" + testCategoryId)
            .headers(headers)
            .build();
        
        ResponseEntity<AdminCategoryResponse> response = restTemplate.exchange(
            request, AdminCategoryResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(testCategoryId);
        assertThat(response.getBody().name()).isEqualTo("Fiction");
    }

    @Test
    void shouldReturn404ForNonExistentCategory() {
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<Void> request = RequestEntity.get("/admin/categories/99999")
            .headers(headers)
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ========== Create Tests (3) ==========

    @Test
    void shouldCreateCategory() {
        CategoryCreateRequest createRequest = new CategoryCreateRequest(
            "Science Fiction",
            "Science fiction books"
        );
        
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<CategoryCreateRequest> request = RequestEntity.post("/admin/categories")
            .headers(headers)
            .body(createRequest);
        
        ResponseEntity<AdminCategoryResponse> response = restTemplate.exchange(
            request, AdminCategoryResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Science Fiction");
        assertThat(response.getBody().description()).isEqualTo("Science fiction books");
        
        // Verify in database
        Category saved = categoryRepository.findById(response.getBody().id()).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Science Fiction");
    }

    @Test
    void shouldRejectDuplicateCategoryName() {
        CategoryCreateRequest createRequest = new CategoryCreateRequest(
            "Fiction", // Already exists
            "Duplicate category"
        );
        
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<CategoryCreateRequest> request = RequestEntity.post("/admin/categories")
            .headers(headers)
            .body(createRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("BUSINESS_RULE_VIOLATION");
    }

    @Test
    void shouldRejectCategoryWithMissingName() {
        String json = "{\"description\": \"Missing name\"}";
        
        HttpHeaders headers = createAuthHeaders(adminToken);
        headers.set("Content-Type", "application/json");
        RequestEntity<String> request = RequestEntity.post("/admin/categories")
            .headers(headers)
            .body(json);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("VALIDATION_ERROR");
    }

    // ========== Update Tests (3) ==========

    @Test
    void shouldUpdateCategory() {
        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest(
            "Fiction Books",
            "Updated description"
        );
        
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<CategoryUpdateRequest> request = RequestEntity.put("/admin/categories/" + testCategoryId)
            .headers(headers)
            .body(updateRequest);
        
        ResponseEntity<AdminCategoryResponse> response = restTemplate.exchange(
            request, AdminCategoryResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Fiction Books");
        assertThat(response.getBody().description()).isEqualTo("Updated description");
        
        // Verify in database
        Category updated = categoryRepository.findById(testCategoryId).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Fiction Books");
    }

    @Test
    void shouldRejectUpdateWithDuplicateName() {
        // Create another category
        Category anotherCategory = Category.builder()
            .name("Non-Fiction")
            .description("Non-fiction books")
            .build();
        categoryRepository.save(anotherCategory);
        
        // Try to update testCategory with existing name
        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest(
            "Non-Fiction",
            "Trying to use existing name"
        );
        
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<CategoryUpdateRequest> request = RequestEntity.put("/admin/categories/" + testCategoryId)
            .headers(headers)
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("BUSINESS_RULE_VIOLATION");
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentCategory() {
        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest(
            "Non Existent",
            "Does not exist"
        );
        
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<CategoryUpdateRequest> request = RequestEntity.put("/admin/categories/99999")
            .headers(headers)
            .body(updateRequest);
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ========== Delete Tests (2) ==========

    @Test
    void shouldDeleteCategory() {
        // Create a new category to delete
        Category toDelete = Category.builder()
            .name("To Delete")
            .description("Will be deleted")
            .build();
        toDelete = categoryRepository.save(toDelete);
        
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<Void> request = RequestEntity.delete("/admin/categories/" + toDelete.getId())
            .headers(headers)
            .build();
        
        ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Verify deletion in database
        assertThat(categoryRepository.findById(toDelete.getId())).isEmpty();
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentCategory() {
        HttpHeaders headers = createAuthHeaders(adminToken);
        RequestEntity<Void> request = RequestEntity.delete("/admin/categories/99999")
            .headers(headers)
            .build();
        
        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
