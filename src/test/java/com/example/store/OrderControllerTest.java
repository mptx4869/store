package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.store.dto.CartItemRequest;
import com.example.store.dto.LoginRequest;
import com.example.store.dto.OrderCreateRequest;
import com.example.store.dto.OrderResponse;
import com.example.store.model.Book;
import com.example.store.model.ProductSku;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.AuthorRepository;
import com.example.store.repository.BookAuthorRepository;
import com.example.store.repository.BookCategoryRepository;
import com.example.store.repository.BookMediaRepository;
import com.example.store.repository.BookRepository;
import com.example.store.repository.CartItemRepository;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.InventoryRepository;
import com.example.store.repository.OrderItemRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductSkuRepository;
import com.example.store.repository.PublisherRepository;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.ShoppingCartRepository;
import com.example.store.repository.UserRepository;
import com.example.store.SetUpTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BookMediaRepository bookMediaRepository;

    @Autowired
    private BookAuthorRepository bookAuthorRepository;

    @Autowired
    private BookCategoryRepository bookCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired 
    private SetUpTest setUpTest;

    private Long bookId;
    private Long userId;

    @BeforeEach
    void setUp() {
        setUpTest.setUp();

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
        userId = userRepository.save(baseUser).getId();

        Book book = Book.builder()
            .title("Order Book")
            .description("Book for order testing")
            .language("EN")
            .pages(220)
            .publishedDate(java.time.LocalDate.of(2020, 1, 10))
            .basePrice(new BigDecimal("20.00"))
            .build();
        book = bookRepository.save(book);

        ProductSku sku = productSkuRepository.save(ProductSku.builder()
            .book(book)
            .sku("ORDER-SKU")
            .format("PAPERBACK")
            .priceOverride(new BigDecimal("20.00"))
            .build());

        book.setDefaultSkuId(sku.getId());
        bookRepository.save(book);
        bookId = book.getId();
    }

    @Test
    void shouldCreateOrderAndReturnHistory() {
        String token = loginAndGetToken();

        addItemToCart(token, 2);

        ResponseEntity<OrderResponse> createResponse = restTemplate.exchange(
            RequestEntity.post("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new OrderCreateRequest(null, null, null)),
            OrderResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OrderResponse createdOrder = createResponse.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getItems()).hasSize(1);
        assertThat(createdOrder.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(createdOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(createdOrder.getStatus()).isEqualTo("PLACED");

        ResponseEntity<OrderResponse[]> historyResponse = restTemplate.exchange(
            RequestEntity.get("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build(),
            OrderResponse[].class);

        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderResponse[] history = historyResponse.getBody();
        assertThat(history).isNotNull();
        assertThat(history).hasSize(1);
        assertThat(history[0].getOrderId()).isEqualTo(createdOrder.getOrderId());

        assertThat(shoppingCartRepository.findByUserIdAndStatus(userId, "ACTIVE")).isEmpty();
    }

    private void addItemToCart(String token, int quantity) {
        ResponseEntity<Map> response = restTemplate.exchange(
            RequestEntity.post("/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new CartItemRequest(bookId, quantity)),
            Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private String loginAndGetToken() {
        ResponseEntity<Map> loginResponse = restTemplate
            .postForEntity("/login", new LoginRequest("nhanhoa", "123456"), Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) loginResponse.getBody().get("token");
    }
}
