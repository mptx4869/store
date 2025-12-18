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
            .basePrice(new BigDecimal("15.50"))
            .build();
        book = bookRepository.save(book);

        ProductSku sku = productSkuRepository.save(ProductSku.builder()
            .book(book)
            .sku("TEST-SKU")
            .format("HARDCOVER")
            .priceOverride(new BigDecimal("15.50"))
            .build());

        book.setDefaultSkuId(sku.getId());
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
        assertThat(response.getBody()).containsEntry("title", "Test Book");
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
