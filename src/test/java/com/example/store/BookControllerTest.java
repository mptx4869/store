package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.store.model.Book;
import com.example.store.model.ProductSku;
import com.example.store.repository.BookRepository;
import com.example.store.repository.ProductSkuRepository;

import com.example.store.SetUpTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired 
    SetUpTest setUpTest;

    private Long existingBookId;

    @BeforeEach
    void setUp() {
        setUpTest.setUp();
        productSkuRepository.deleteAll();
        bookRepository.deleteAll();
        Book book = Book.builder()
            .title("Test Book")
            .description("A sample test book")
            .language("EN")
            .pages(200)
            .publishedDate(LocalDate.of(2020, 1, 1))
            .imageUrl("https://cdn.example.com/test-book.jpg")
            .basePrice(new BigDecimal("15.50"))
            .build();
        book = bookRepository.save(book);

        // Create multiple SKUs
        ProductSku paperbackSku = productSkuRepository.save(ProductSku.builder()
            .book(book)
            .sku("TEST-SKU-PB")
            .format("Paperback")
            .priceOverride(new BigDecimal("15.50"))
            .weightGrams(300)
            .lengthMm(200)
            .widthMm(130)
            .heightMm(15)
            .build());

        ProductSku hardcoverSku = productSkuRepository.save(ProductSku.builder()
            .book(book)
            .sku("TEST-SKU-HB")
            .format("Hardcover")
            .priceOverride(new BigDecimal("25.00"))
            .weightGrams(500)
            .lengthMm(210)
            .widthMm(140)
            .heightMm(20)
            .build());

        book.setDefaultSkuId(paperbackSku.getId());
        bookRepository.save(book);
        existingBookId = book.getId();
    }

    @Test
    void shouldReturnListOfBooks() {
        ResponseEntity<Map[]> response = restTemplate.getForEntity("/books", Map[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void shouldReturnSingleBook() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/books/{id}", Map.class, existingBookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .containsEntry("title", "Test Book")
            .containsEntry("imageUrl", "https://cdn.example.com/test-book.jpg");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnAllSkusInBookResponse() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/books/{id}", Map.class, existingBookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("skus");
        
        var skus = (java.util.List<Map<String, Object>>) response.getBody().get("skus");
        assertThat(skus).hasSize(2);
        
        // Verify Paperback SKU (default)
        Map<String, Object> paperback = skus.stream()
            .filter(s -> "TEST-SKU-PB".equals(s.get("sku")))
            .findFirst()
            .orElseThrow();
        
        assertThat(paperback)
            .containsEntry("sku", "TEST-SKU-PB")
            .containsEntry("format", "Paperback")
            .containsEntry("price", 15.50)
            .containsEntry("isDefault", true)
            .containsEntry("inStock", false)
            .containsEntry("availableStock", 0)
            .containsEntry("weightGrams", 300)
            .containsEntry("lengthMm", 200);
        
        // Verify Hardcover SKU
        Map<String, Object> hardcover = skus.stream()
            .filter(s -> "TEST-SKU-HB".equals(s.get("sku")))
            .findFirst()
            .orElseThrow();
        
        assertThat(hardcover)
            .containsEntry("sku", "TEST-SKU-HB")
            .containsEntry("format", "Hardcover")
            .containsEntry("price", 25.00)
            .containsEntry("isDefault", false)
            .containsEntry("inStock", false)
            .containsEntry("availableStock", 0)
            .containsEntry("weightGrams", 500);
    }

    @Test
    void shouldReturnNotFoundForMissingBook() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/books/{id}", Map.class, 9999);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
            .containsEntry("error", "Resource Not Found")
            .containsEntry("message", "Book not found")
            .containsEntry("code", "RESOURCE_NOT_FOUND");
    }
}
