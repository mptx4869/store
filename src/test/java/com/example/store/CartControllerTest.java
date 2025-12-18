package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

import com.example.store.dto.CartItemRequest;
import com.example.store.dto.LoginRequest;
import com.example.store.model.Book;
import com.example.store.model.CartItem;
import com.example.store.model.Role;
import com.example.store.model.ShoppingCart;
import com.example.store.model.User;
import com.example.store.model.ProductSku;

import com.example.store.repository.BookRepository;

import com.example.store.repository.ProductSkuRepository;
import com.example.store.repository.PublisherRepository;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.ShoppingCartRepository;
import com.example.store.repository.UserRepository;
import com.example.store.SetUpTest;

import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SetUpTest setUpTest;

    @Autowired
    ShoppingCartRepository shoppingCartRepo;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Long bookId;

    @BeforeEach
    void setUp() {
        // Clean up graph respecting foreign keys
        setUpTest.setUp();

    // Create user and role
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        Role customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole = roleRepository.save(customerRole);

        User baseUser = User.builder()
            .username("nhanhoa")
            .passwordHash(passwordEncoder.encode("123456"))
            .email("hoa@example.com")
            .role(customerRole)
            .status("ACTIVE")
            .build();
        userRepository.save(baseUser);
    // Create a book witikh a default SKU

        Book book = Book.builder()
            .title("Cart Book")
            .description("Book for cart testing")
            .language("EN")
            .pages(280)
            .publishedDate(java.time.LocalDate.of(2021, 5, 15))
            .basePrice(new BigDecimal("20.00"))
            .build();
        book = bookRepository.save(book);

        ProductSku sku = productSkuRepository.save(ProductSku.builder()
            .book(book)
            .sku("CART-SKU")
            .format("PAPERBACK")
            .priceOverride(new BigDecimal("20.00"))
            .build());

        book.setDefaultSkuId(sku.getId());
        bookRepository.save(book);
        bookId = book.getId();

        // Create Inventory using JdbcTemplate to avoid JPA @MapsId issues
        jdbcTemplate.update(
            "INSERT INTO inventory (sku_id, stock, reserved, last_updated) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
            sku.getId(), 100, 0
        );
    }

    @Test
    void shouldAddItemToCart() {
        String token = loginAndGetToken();

        RequestEntity<CartItemRequest> request = RequestEntity
            .post("/cart/items")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .body(new CartItemRequest(bookId, 2));

        ResponseEntity<Map> response = restTemplate.exchange(request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("cartId")).isNotNull();

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("quantity")).isEqualTo(2);
        assertThat(response.getBody().get("totalItems")).isEqualTo(2);
    }

    @Test
    void shouldIncrementQuantityWhenAddingExistingItem() {
        String token = loginAndGetToken();

        RequestEntity<CartItemRequest> firstRequest = RequestEntity
            .post("/cart/items")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .body(new CartItemRequest(bookId, 1));
        restTemplate.exchange(firstRequest, Map.class);

        RequestEntity<CartItemRequest> secondRequest = RequestEntity
            .post("/cart/items")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .body(new CartItemRequest(bookId, 3));

        ResponseEntity<Map> response = restTemplate.exchange(secondRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("quantity")).isEqualTo(4);
        assertThat(response.getBody().get("totalItems")).isEqualTo(4);
    }

    @Test
    void shouldGetCartContents() {
        String token = loginAndGetToken();

        // Add item to cart first
        RequestEntity<CartItemRequest> addItemRequest = RequestEntity
            .post("/cart/items")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .body(new CartItemRequest(bookId, 2));
        restTemplate.exchange(addItemRequest, Map.class);

        // Now get the cart
        RequestEntity<Void> getCartRequest = RequestEntity
            .get("/cart")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();

        ResponseEntity<Map> response = restTemplate.exchange(getCartRequest, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("cartId")).isNotNull();

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("quantity")).isEqualTo(2);
        assertThat(response.getBody().get("totalItems")).isEqualTo(2);
    }

    private String loginAndGetToken() {
        ResponseEntity<Map> loginResponse = restTemplate
            .postForEntity("/login", new LoginRequest("nhanhoa", "123456"), Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) loginResponse.getBody().get("token");
    }
}
