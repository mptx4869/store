package com.example.store;

import org.springframework.beans.factory.annotation.Autowired;

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

import org.springframework.stereotype.Component;

@Component
class SetUpTest {

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
    void setUp() {
        // Clean up graph respecting foreign keys
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        cartItemRepository.deleteAllInBatch();
        shoppingCartRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        bookMediaRepository.deleteAllInBatch();
        bookAuthorRepository.deleteAllInBatch();
        bookCategoryRepository.deleteAllInBatch();
        productSkuRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        authorRepository.deleteAllInBatch();
        publisherRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();

    }
}
