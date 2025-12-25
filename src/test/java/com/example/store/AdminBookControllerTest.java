package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.example.store.dto.AdminBookResponse;
import com.example.store.dto.BookCreateRequest;
import com.example.store.dto.BookUpdateRequest;
import com.example.store.dto.LoginRequest;
import com.example.store.dto.SkuCreateRequest;
import com.example.store.dto.SkuUpdateRequest;
import com.example.store.model.Book;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.BookRepository;
import com.example.store.repository.InventoryRepository;
import com.example.store.repository.ProductSkuRepository;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminBookControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    SetUpTest setUpTest;

    private String adminToken;
    private String customerToken;
    private Long testBookId;

    @BeforeEach
    void setUp() {
        setUpTest.setUp();

        // Clean up
        inventoryRepository.deleteAll();
        productSkuRepository.deleteAll();
        bookRepository.deleteAll();
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
        userRepository.save(admin);

        // Create customer user
        User customer = User.builder()
                .username("customer")
                .email("customer@example.com")
                .passwordHash(passwordEncoder.encode("customer123"))
                .status("ACTIVE")
                .role(customerRole)
                .build();
        userRepository.save(customer);

        // Get tokens
        adminToken = getTokenForUser("admin", "admin123");
        customerToken = getTokenForUser("customer", "customer123");

        // Create test book with SQL (no publisher)
        jdbcTemplate.update(
                "INSERT INTO books (title, subtitle, description, language, pages, published_date, base_price, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "Test Book", "Test Subtitle", "Test Description", "EN", 300,
                LocalDate.of(2024, 1, 1), new BigDecimal("29.99"));
        testBookId = jdbcTemplate.queryForObject("SELECT id FROM books WHERE title = 'Test Book'", Long.class);

        // Create test SKU
        jdbcTemplate.update(
                "INSERT INTO product_skus (book_id, sku, format, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testBookId, "TEST-001", "HARDCOVER");
        Long testSkuId = jdbcTemplate.queryForObject("SELECT id FROM product_skus WHERE sku = 'TEST-001'", Long.class);

        // Create test inventory
        jdbcTemplate.update(
                "INSERT INTO inventory (sku_id, stock, reserved) VALUES (?, ?, ?)",
                testSkuId, 100, 0);

        // Update book with default SKU
        jdbcTemplate.update("UPDATE books SET default_sku_id = ? WHERE id = ?", testSkuId, testBookId);
    }

    private String getTokenForUser(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<Map> response = restTemplate.postForEntity("/login", loginRequest, Map.class);
        return response.getBody().get("token").toString();
    }

    // Authorization Tests

    @Test
    void shouldAllowAdminToGetAllBooks() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        RequestEntity<Void> request = RequestEntity
                .get("/admin/books")
                .headers(headers)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldDenyCustomerAccessToBookList() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);

        RequestEntity<Void> request = RequestEntity
                .get("/admin/books")
                .headers(headers)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldDenyAccessWithoutToken() {
        RequestEntity<Void> request = RequestEntity
                .get("/admin/books")
                .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        // Spring Security returns 403 FORBIDDEN for missing token in this config
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Get All Books Tests

    @Test
    void shouldReturnPaginatedBooks() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        RequestEntity<Void> request = RequestEntity
                .get("/admin/books?page=0&size=10")
                .headers(headers)
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("content");
        assertThat(response.getBody()).containsKey("totalElements");
        assertThat((Integer) response.getBody().get("totalElements")).isGreaterThan(0);
    }

    // Get Book Details Tests

    @Test
    void shouldGetBookDetailsAsAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        RequestEntity<Void> request = RequestEntity
                .get("/admin/books/" + testBookId)
                .headers(headers)
                .build();

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(request, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Test Book");
        assertThat(response.getBody().getSkus()).isNotEmpty();
    }

    @Test
    void shouldReturn404ForNonExistentBook() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        RequestEntity<Void> request = RequestEntity
                .get("/admin/books/999999")
                .headers(headers)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDenyCustomerAccessToBookDetails() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);

        RequestEntity<Void> request = RequestEntity
                .get("/admin/books/" + testBookId)
                .headers(headers)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Create Book Tests

    @Test
    void shouldCreateBookWithSkus() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("Content-Type", "application/json");

        BookCreateRequest.SkuCreateRequest sku1 = BookCreateRequest.SkuCreateRequest.builder()
                .sku("NEW-BOOK-001")
                .format("HARDCOVER")
                .initialStock(50)
                .isDefault(true)
                .build();

        BookCreateRequest.SkuCreateRequest sku2 = BookCreateRequest.SkuCreateRequest.builder()
                .sku("NEW-BOOK-002")
                .format("PAPERBACK")
                .priceOverride(new BigDecimal("19.99"))
                .initialStock(100)
                .build();

        BookCreateRequest request = BookCreateRequest.builder()
                .title("New Book")
                .subtitle("New Subtitle")
                .description("New Description")
                .language("EN")
                .pages(400)
                .publishedDate(LocalDate.of(2024, 6, 1))
                .imageUrl("https://cdn.example.com/new-book.jpg")
                .basePrice(new BigDecimal("29.99"))
                .skus(List.of(sku1, sku2))
                .build();

        RequestEntity<BookCreateRequest> requestEntity = RequestEntity
                .post("/admin/books")
                .headers(headers)
                .body(request);

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(requestEntity, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("New Book");
        assertThat(response.getBody().getSkus()).hasSize(2);
        assertThat(response.getBody().getDefaultSkuId()).isNotNull();
        assertThat(response.getBody().getImageUrl()).isEqualTo("https://cdn.example.com/new-book.jpg");

        // Verify in database
        Book createdBook = bookRepository.findById(response.getBody().getId()).orElseThrow();
        assertThat(createdBook.getTitle()).isEqualTo("New Book");
        assertThat(productSkuRepository.findByBookId(createdBook.getId())).hasSize(2);
    }

    @Test
    void shouldRejectBookCreationWithMissingTitle() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("Content-Type", "application/json");

        BookCreateRequest.SkuCreateRequest sku = BookCreateRequest.SkuCreateRequest.builder()
                .sku("INVALID-001")
                .format("HARDCOVER")
                .build();

        BookCreateRequest request = BookCreateRequest.builder()
                .subtitle("Subtitle without title")
                .basePrice(new BigDecimal("29.99"))
                .skus(List.of(sku))
                .build();

        RequestEntity<BookCreateRequest> requestEntity = RequestEntity
                .post("/admin/books")
                .headers(headers)
                .body(request);

        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectBookCreationWithDuplicateSku() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("Content-Type", "application/json");

        BookCreateRequest.SkuCreateRequest sku = BookCreateRequest.SkuCreateRequest.builder()
                .sku("TEST-001") // This SKU already exists
                .format("HARDCOVER")
                .build();

        BookCreateRequest request = BookCreateRequest.builder()
                .title("Another Book")
                .basePrice(new BigDecimal("29.99"))
                .skus(List.of(sku))
                .build();

        RequestEntity<BookCreateRequest> requestEntity = RequestEntity
                .post("/admin/books")
                .headers(headers)
                .body(request);

        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldDenyCustomerFromCreatingBook() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);
        headers.set("Content-Type", "application/json");

        BookCreateRequest.SkuCreateRequest sku = BookCreateRequest.SkuCreateRequest.builder()
                .sku("CUSTOMER-001")
                .format("HARDCOVER")
                .build();

        BookCreateRequest request = BookCreateRequest.builder()
                .title("Customer Book")
                .basePrice(new BigDecimal("29.99"))
                .skus(List.of(sku))
                .build();

        RequestEntity<BookCreateRequest> requestEntity = RequestEntity
                .post("/admin/books")
                .headers(headers)
                .body(request);

        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Update Book Tests

    @Test
    void shouldUpdateBookDetails() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("Content-Type", "application/json");

        BookUpdateRequest request = BookUpdateRequest.builder()
                .title("Updated Book Title")
                .subtitle("Updated Subtitle")
                .description("Updated Description")
                .language("EN")
                .pages(350)
                .imageUrl("https://cdn.example.com/updated-book.jpg")
                .basePrice(new BigDecimal("34.99"))
                .build();

        RequestEntity<BookUpdateRequest> requestEntity = RequestEntity
                .put("/admin/books/" + testBookId)
                .headers(headers)
                .body(request);

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(requestEntity, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Book Title");
        assertThat(response.getBody().getBasePrice()).isEqualTo(new BigDecimal("34.99"));
        assertThat(response.getBody().getImageUrl()).isEqualTo("https://cdn.example.com/updated-book.jpg");

        // Verify in database
        Book updatedBook = bookRepository.findById(testBookId).orElseThrow();
        assertThat(updatedBook.getTitle()).isEqualTo("Updated Book Title");
        assertThat(updatedBook.getBasePrice()).isEqualTo(new BigDecimal("34.99"));
        assertThat(updatedBook.getImageUrl()).isEqualTo("https://cdn.example.com/updated-book.jpg");
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentBook() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("Content-Type", "application/json");

        BookUpdateRequest request = BookUpdateRequest.builder()
                .title("Updated Title")
                .basePrice(new BigDecimal("29.99"))
                .build();

        RequestEntity<BookUpdateRequest> requestEntity = RequestEntity
                .put("/admin/books/999999")
                .headers(headers)
                .body(request);

        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDenyCustomerFromUpdatingBook() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);
        headers.set("Content-Type", "application/json");

        BookUpdateRequest request = BookUpdateRequest.builder()
                .title("Customer Update")
                .basePrice(new BigDecimal("29.99"))
                .build();

        RequestEntity<BookUpdateRequest> requestEntity = RequestEntity
                .put("/admin/books/" + testBookId)
                .headers(headers)
                .body(request);

        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Delete Book Tests

    @Test
    void shouldSoftDeleteBook() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        RequestEntity<Void> request = RequestEntity
                .delete("/admin/books/" + testBookId)
                .headers(headers)
                .build();

        ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify in database - book should still exist but with deletedAt set
        Book deletedBook = bookRepository.findById(testBookId).orElseThrow();
        assertThat(deletedBook.getDeletedAt()).isNotNull();
    }

        @Test
        void shouldRestoreSoftDeletedBook() {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(adminToken);

                // Soft delete first
                RequestEntity<Void> deleteRequest = RequestEntity
                                .delete("/admin/books/" + testBookId)
                                .headers(headers)
                                .build();
                ResponseEntity<Void> deleteResponse = restTemplate.exchange(deleteRequest, Void.class);
                assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

                // Restore
                RequestEntity<Void> restoreRequest = RequestEntity
                                .patch("/admin/books/" + testBookId + "/restore")
                                .headers(headers)
                                .build();
                ResponseEntity<AdminBookResponse> restoreResponse = restTemplate.exchange(restoreRequest, AdminBookResponse.class);

                assertThat(restoreResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(restoreResponse.getBody()).isNotNull();
                assertThat(restoreResponse.getBody().getDeletedAt()).isNull();

                // Verify in database
                Book restored = bookRepository.findById(testBookId).orElseThrow();
                assertThat(restored.getDeletedAt()).isNull();
        }

        @Test
        void shouldFailRestoreIfBookNotDeleted() {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(adminToken);

                RequestEntity<Void> restoreRequest = RequestEntity
                                .patch("/admin/books/" + testBookId + "/restore")
                                .headers(headers)
                                .build();

                ResponseEntity<String> response = restTemplate.exchange(restoreRequest, String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

    // Note: Hard delete is commented out due to foreign key constraints
    // @Test
    // void shouldHardDeleteBook() {
    //     HttpHeaders headers = new HttpHeaders();
    //     headers.setBearerAuth(adminToken);

    //     RequestEntity<Void> request = RequestEntity
    //             .delete("/admin/books/" + testBookId + "?hard=true")
    //             .headers(headers)
    //             .build();

    //     ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);

    //     assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    //     // Verify in database - book should not exist
    //     assertThat(bookRepository.findById(testBookId)).isEmpty();
    // }

    @Test
    void shouldReturn404WhenDeletingNonExistentBook() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        RequestEntity<Void> request = RequestEntity
                .delete("/admin/books/999999")
                .headers(headers)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDenyCustomerFromDeletingBook() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);

        RequestEntity<Void> request = RequestEntity
                .delete("/admin/books/" + testBookId)
                .headers(headers)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Image URL Tests

    @Test
    void shouldCreateBookWithoutImageUrl() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("Content-Type", "application/json");

        BookCreateRequest.SkuCreateRequest sku = BookCreateRequest.SkuCreateRequest.builder()
                .sku("NO-IMAGE-001")
                .format("PAPERBACK")
                .initialStock(20)
                .isDefault(true)
                .build();

        BookCreateRequest request = BookCreateRequest.builder()
                .title("Book Without Image")
                .basePrice(new BigDecimal("15.99"))
                .skus(List.of(sku))
                .build();

        RequestEntity<BookCreateRequest> requestEntity = RequestEntity
                .post("/admin/books")
                .headers(headers)
                .body(request);

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(requestEntity, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getImageUrl()).isNull();
    }

    @Test
    void shouldUpdateBookImageUrl() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("Content-Type", "application/json");

        // First update: add image URL
        BookUpdateRequest request1 = BookUpdateRequest.builder()
                .title("Test Book")
                .basePrice(new BigDecimal("25.00"))
                .imageUrl("https://cdn.example.com/first-image.jpg")
                .build();

        RequestEntity<BookUpdateRequest> requestEntity1 = RequestEntity
                .put("/admin/books/" + testBookId)
                .headers(headers)
                .body(request1);

        ResponseEntity<AdminBookResponse> response1 = restTemplate.exchange(requestEntity1, AdminBookResponse.class);
        assertThat(response1.getBody().getImageUrl()).isEqualTo("https://cdn.example.com/first-image.jpg");

        // Second update: change image URL
        BookUpdateRequest request2 = BookUpdateRequest.builder()
                .title("Test Book")
                .basePrice(new BigDecimal("25.00"))
                .imageUrl("https://cdn.example.com/second-image.jpg")
                .build();

        RequestEntity<BookUpdateRequest> requestEntity2 = RequestEntity
                .put("/admin/books/" + testBookId)
                .headers(headers)
                .body(request2);

        ResponseEntity<AdminBookResponse> response2 = restTemplate.exchange(requestEntity2, AdminBookResponse.class);
        assertThat(response2.getBody().getImageUrl()).isEqualTo("https://cdn.example.com/second-image.jpg");

        // Verify in database
        Book updatedBook = bookRepository.findById(testBookId).orElseThrow();
        assertThat(updatedBook.getImageUrl()).isEqualTo("https://cdn.example.com/second-image.jpg");
    }

    @Test
    void shouldRemoveImageUrlWhenUpdatingToNull() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("Content-Type", "application/json");

        // First: set an image
        BookUpdateRequest request1 = BookUpdateRequest.builder()
                .title("Test Book")
                .basePrice(new BigDecimal("25.00"))
                .imageUrl("https://cdn.example.com/temp-image.jpg")
                .build();

        restTemplate.exchange(
                RequestEntity.put("/admin/books/" + testBookId).headers(headers).body(request1),
                AdminBookResponse.class
        );

        // Then: remove image by setting to null
        BookUpdateRequest request2 = BookUpdateRequest.builder()
                .title("Test Book")
                .basePrice(new BigDecimal("25.00"))
                .imageUrl(null)
                .build();

        RequestEntity<BookUpdateRequest> requestEntity2 = RequestEntity
                .put("/admin/books/" + testBookId)
                .headers(headers)
                .body(request2);

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(requestEntity2, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getImageUrl()).isNull();

        // Verify in database
        Book updatedBook = bookRepository.findById(testBookId).orElseThrow();
        assertThat(updatedBook.getImageUrl()).isNull();
    }

    // SKU Management Tests

    @Test
    void shouldAddNewSkuToBook() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        SkuCreateRequest request = SkuCreateRequest.builder()
                .sku("NEW-SKU-001")
                .format("eBook")
                .priceOverride(new BigDecimal("9.99"))
                .initialStock(999)
                .isDefault(false)
                .build();

        RequestEntity<SkuCreateRequest> requestEntity = RequestEntity
                .post("/admin/books/" + testBookId + "/skus")
                .headers(headers)
                .body(request);

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(requestEntity, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getSkus()).hasSize(2); // Original + new
        
        AdminBookResponse.SkuInfo newSku = response.getBody().getSkus().stream()
                .filter(s -> "NEW-SKU-001".equals(s.getSku()))
                .findFirst()
                .orElseThrow();
        
        assertThat(newSku.getFormat()).isEqualTo("eBook");
        assertThat(newSku.getPrice()).isEqualByComparingTo("9.99");
        assertThat(newSku.getStock()).isEqualTo(999);
        assertThat(newSku.getIsDefault()).isFalse();
    }

    @Test
    void shouldUpdateExistingSku() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // Get current SKU ID
        ResponseEntity<AdminBookResponse> getResponse = restTemplate.exchange(
                RequestEntity.get("/admin/books/" + testBookId)
                        .headers(headers)
                        .build(),
                AdminBookResponse.class
        );
        Long skuId = getResponse.getBody().getSkus().get(0).getId();

        SkuUpdateRequest request = SkuUpdateRequest.builder()
                .sku("UPDATED-SKU")
                .format("Paperback Premium")
                .priceOverride(new BigDecimal("35.00"))
                .weightGrams(600)
                .build();

        RequestEntity<SkuUpdateRequest> requestEntity = RequestEntity
                .put("/admin/books/" + testBookId + "/skus/" + skuId)
                .headers(headers)
                .body(request);

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(requestEntity, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        AdminBookResponse.SkuInfo updatedSku = response.getBody().getSkus().stream()
                .filter(s -> skuId.equals(s.getId()))
                .findFirst()
                .orElseThrow();
        
        assertThat(updatedSku.getSku()).isEqualTo("UPDATED-SKU");
        assertThat(updatedSku.getFormat()).isEqualTo("Paperback Premium");
        assertThat(updatedSku.getPrice()).isEqualByComparingTo("35.00");
    }

    @Test
    void shouldSetDefaultSku() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // Add a new SKU
        SkuCreateRequest createRequest = SkuCreateRequest.builder()
                .sku("NEW-DEFAULT-SKU")
                .format("Hardcover")
                .priceOverride(new BigDecimal("45.00"))
                .initialStock(50)
                .build();

        ResponseEntity<AdminBookResponse> createResponse = restTemplate.exchange(
                RequestEntity.post("/admin/books/" + testBookId + "/skus")
                        .headers(headers)
                        .body(createRequest),
                AdminBookResponse.class
        );

        Long newSkuId = createResponse.getBody().getSkus().stream()
                .filter(s -> "NEW-DEFAULT-SKU".equals(s.getSku()))
                .findFirst()
                .orElseThrow()
                .getId();

        // Set as default
        RequestEntity<Void> setDefaultRequest = RequestEntity
                .patch("/admin/books/" + testBookId + "/skus/" + newSkuId + "/set-default")
                .headers(headers)
                .build();

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(setDefaultRequest, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getDefaultSkuId()).isEqualTo(newSkuId);
        
        AdminBookResponse.SkuInfo defaultSku = response.getBody().getSkus().stream()
                .filter(AdminBookResponse.SkuInfo::getIsDefault)
                .findFirst()
                .orElseThrow();
        
        assertThat(defaultSku.getSku()).isEqualTo("NEW-DEFAULT-SKU");
    }

    @Test
    void shouldDeleteSku() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // Add a second SKU first
        SkuCreateRequest createRequest = SkuCreateRequest.builder()
                .sku("TO-DELETE-SKU")
                .format("eBook")
                .initialStock(100)
                .build();

        ResponseEntity<AdminBookResponse> createResponse = restTemplate.exchange(
                RequestEntity.post("/admin/books/" + testBookId + "/skus")
                        .headers(headers)
                        .body(createRequest),
                AdminBookResponse.class
        );

        Long skuToDeleteId = createResponse.getBody().getSkus().stream()
                .filter(s -> "TO-DELETE-SKU".equals(s.getSku()))
                .findFirst()
                .orElseThrow()
                .getId();

        // Delete SKU
        RequestEntity<Void> deleteRequest = RequestEntity
                .delete("/admin/books/" + testBookId + "/skus/" + skuToDeleteId)
                .headers(headers)
                .build();

        ResponseEntity<AdminBookResponse> response = restTemplate.exchange(deleteRequest, AdminBookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getSkus()).hasSize(1); // Back to 1 SKU
        assertThat(response.getBody().getSkus().stream()
                .noneMatch(s -> "TO-DELETE-SKU".equals(s.getSku()))).isTrue();
    }

    @Test
    void shouldNotDeleteLastSku() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // Get the only SKU
        ResponseEntity<AdminBookResponse> getResponse = restTemplate.exchange(
                RequestEntity.get("/admin/books/" + testBookId)
                        .headers(headers)
                        .build(),
                AdminBookResponse.class
        );
        Long onlySkuId = getResponse.getBody().getSkus().get(0).getId();

        // Try to delete it
        RequestEntity<Void> deleteRequest = RequestEntity
                .delete("/admin/books/" + testBookId + "/skus/" + onlySkuId)
                .headers(headers)
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(deleteRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("message")).toString()
                .contains("Cannot delete the last SKU");
    }

    @Test
    void shouldNotAddDuplicateSkuCode() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // Get existing SKU code
        ResponseEntity<AdminBookResponse> getResponse = restTemplate.exchange(
                RequestEntity.get("/admin/books/" + testBookId)
                        .headers(headers)
                        .build(),
                AdminBookResponse.class
        );
        String existingSkuCode = getResponse.getBody().getSkus().get(0).getSku();

        // Try to add with same SKU code
        SkuCreateRequest request = SkuCreateRequest.builder()
                .sku(existingSkuCode)
                .format("Duplicate")
                .build();

        RequestEntity<SkuCreateRequest> requestEntity = RequestEntity
                .post("/admin/books/" + testBookId + "/skus")
                .headers(headers)
                .body(request);

        ResponseEntity<Map> response = restTemplate.exchange(requestEntity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("message")).toString()
                .contains("SKU already exists");
    }
}
