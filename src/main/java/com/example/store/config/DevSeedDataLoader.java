package com.example.store.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.model.Book;
import com.example.store.model.BookCategory;
import com.example.store.model.BookCategoryId;
import com.example.store.model.CartItem;
import com.example.store.model.Category;
import com.example.store.model.Inventory;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.ProductSku;
import com.example.store.model.Role;
import com.example.store.model.ShoppingCart;
import com.example.store.model.User;
import com.example.store.repository.BookCategoryRepository;
import com.example.store.repository.BookRepository;
import com.example.store.repository.CartItemRepository;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.InventoryRepository;
import com.example.store.repository.OrderItemRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductSkuRepository;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.ShoppingCartRepository;
import com.example.store.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Profile("dev")
@Transactional
public class DevSeedDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedDataLoader.class);

    private final ObjectMapper objectMapper;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final ProductSkuRepository productSkuRepository;
    private final InventoryRepository inventoryRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("classpath:data/dev-seed.json")
    private Resource seedResource;

    public DevSeedDataLoader(
            ObjectMapper objectMapper,
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            BookRepository bookRepository,
            ProductSkuRepository productSkuRepository,
            InventoryRepository inventoryRepository,
            BookCategoryRepository bookCategoryRepository,
            ShoppingCartRepository shoppingCartRepository,
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {
        this.objectMapper = objectMapper;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.productSkuRepository = productSkuRepository;
        this.inventoryRepository = inventoryRepository;
        this.bookCategoryRepository = bookCategoryRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (seedResource == null || !seedResource.exists()) {
            log.warn("Dev seed resource not found; skipping dev data load");
            return;
        }

        SeedData seedData = readSeedData();
        if (seedData == null) {
            log.warn("Unable to deserialize dev seed resource; skipping");
            return;
        }

        Map<Long, Role> roles = seedRoles(seedData.roles());
        Map<Long, Category> categories = seedCategories(seedData.categories());
        Map<Long, User> users = seedUsers(seedData.users(), roles);
        Map<Long, Book> books = seedBooks(seedData.books());
        Map<Long, ProductSku> skus = seedSkus(seedData.productSkus(), books);
        updateBookDefaultSku(seedData.books(), books, skus);
        seedInventory(seedData.inventory(), skus);
        seedBookCategories(seedData.bookCategories(), books, categories);
        Map<Long, ShoppingCart> carts = seedCarts(seedData.shoppingCarts(), users);
        seedCartItems(seedData.cartItems(), carts, skus);
        Map<Long, Order> orders = seedOrders(seedData.orders(), users, carts);
        seedOrderItems(seedData.orderItems(), orders, books, skus);
    }

    private SeedData readSeedData() {
        try {
            return objectMapper.readValue(seedResource.getInputStream(), SeedData.class);
        } catch (IOException ex) {
            log.error("Failed to read dev seed data", ex);
            return null;
        }
    }

    private Map<Long, Role> seedRoles(List<SeedRole> roleSeed) {
        Map<Long, Role> map = new HashMap<>();
        if (roleSeed == null) {
            return map;
        }
        for (SeedRole seed : roleSeed) {
            Role role = roleRepository.findByName(seed.name())
                    .map(existing -> {
                        if (!Objects.equals(existing.getDescription(), seed.description())) {
                            existing.setDescription(seed.description());
                            return roleRepository.save(existing);
                        }
                        return existing;
                    })
                    .orElseGet(() -> {
                        Role created = new Role();
                        created.setName(seed.name());
                        created.setDescription(seed.description());
                        return roleRepository.save(created);
                    });
            if (seed.id() != null) {
                map.put(seed.id(), role);
            }
        }
        return map;
    }

    private Map<Long, Category> seedCategories(List<SeedCategory> categorySeed) {
        Map<Long, Category> map = new HashMap<>();
        if (categorySeed == null) {
            return map;
        }
        for (SeedCategory seed : categorySeed) {
            Category existing = categoryRepository.findByName(seed.name());
            if (existing == null) {
                existing = Category.builder()
                        .name(seed.name())
                        .description(seed.description())
                        .build();
            } else if (!Objects.equals(existing.getDescription(), seed.description())) {
                existing.setDescription(seed.description());
            }
            Category saved = categoryRepository.save(existing);
            if (seed.id() != null) {
                map.put(seed.id(), saved);
            }
        }
        return map;
    }

    private Map<Long, User> seedUsers(List<SeedUser> userSeed, Map<Long, Role> roles) {
        Map<Long, User> map = new HashMap<>();
        if (userSeed == null) {
            return map;
        }
        for (SeedUser seed : userSeed) {
            Optional<User> optional = userRepository.findByUsername(seed.username());
            User user = optional.orElseGet(User::new);
            user.setUsername(seed.username());
            user.setEmail(seed.email());
            user.setPasswordHash(seed.passwordHash());
            user.setStatus(seed.status() != null ? seed.status() : "ACTIVE");
            Role role = roles.get(seed.roleId());
            if (role == null) {
                log.warn("Role id {} not found for user {}", seed.roleId(), seed.username());
                continue;
            }
            user.setRole(role);
            User saved = userRepository.save(user);
            if (seed.id() != null) {
                map.put(seed.id(), saved);
            }
        }
        return map;
    }

    private Map<Long, Book> seedBooks(List<SeedBook> bookSeed) {
        Map<Long, Book> map = new HashMap<>();
        if (bookSeed == null) {
            return map;
        }
        for (SeedBook seed : bookSeed) {
            Book book = bookRepository.findAll().stream()
                    .filter(existing -> existing.getTitle().equalsIgnoreCase(seed.title()))
                    .findFirst()
                    .orElseGet(Book::new);
            book.setTitle(seed.title());
            book.setSubtitle(seed.subtitle());
            book.setDescription(seed.description());
            book.setLanguage(seed.language());
            book.setPages(seed.pages());
            book.setPublishedDate(seed.publishedDate());
            book.setBasePrice(seed.basePrice() != null ? seed.basePrice() : BigDecimal.ZERO);
            Book saved = bookRepository.save(book);
            if (seed.id() != null) {
                map.put(seed.id(), saved);
            }
        }
        return map;
    }

    private Map<Long, ProductSku> seedSkus(List<SeedProductSku> skuSeed, Map<Long, Book> books) {
        Map<Long, ProductSku> map = new HashMap<>();
        if (skuSeed == null) {
            return map;
        }
        for (SeedProductSku seed : skuSeed) {
            Book book = books.get(seed.bookId());
            if (book == null) {
                log.warn("Book id {} not found when seeding sku {}", seed.bookId(), seed.sku());
                continue;
            }
            ProductSku sku = productSkuRepository.findBySku(seed.sku())
                    .orElseGet(ProductSku::new);
            sku.setBook(book);
            sku.setSku(seed.sku());
            sku.setFormat(seed.format());
            sku.setPriceOverride(seed.priceOverride());
            sku.setWeightGrams(seed.weightGrams());
            sku.setLengthMm(seed.lengthMm());
            sku.setWidthMm(seed.widthMm());
            sku.setHeightMm(seed.heightMm());
            ProductSku saved = productSkuRepository.save(sku);
            if (seed.id() != null) {
                map.put(seed.id(), saved);
            }
        }
        return map;
    }

    private void updateBookDefaultSku(List<SeedBook> bookSeed, Map<Long, Book> books, Map<Long, ProductSku> skus) {
        if (bookSeed == null) {
            return;
        }
        for (SeedBook seed : bookSeed) {
            if (seed.defaultSkuId() == null) {
                continue;
            }
            Book book = books.get(seed.id());
            ProductSku defaultSku = skus.get(seed.defaultSkuId());
            if (book != null && defaultSku != null && !Objects.equals(book.getDefaultSkuId(), defaultSku.getId())) {
                book.setDefaultSkuId(defaultSku.getId());
                bookRepository.save(book);
            }
        }
    }

    private void seedInventory(List<SeedInventory> inventorySeed, Map<Long, ProductSku> skus) {
        if (inventorySeed == null) {
            return;
        }
        for (SeedInventory seed : inventorySeed) {
            ProductSku sku = skus.get(seed.skuId());
            if (sku == null) {
                log.warn("SKU id {} not found when seeding inventory", seed.skuId());
                continue;
            }

            Inventory inventory = inventoryRepository.findById(sku.getId())
                    .orElseGet(() -> {
                        Inventory created = new Inventory();
                        created.setProductSku(sku);
                        return created;
                    });
            inventory.setProductSku(sku);
            inventory.setStock(seed.stock() != null ? seed.stock() : 0);
            inventory.setReserved(seed.reserved() != null ? seed.reserved() : 0);
            inventoryRepository.save(inventory);
        }
    }

    private void seedBookCategories(List<SeedBookCategory> seedList, Map<Long, Book> books, Map<Long, Category> categories) {
        if (seedList == null) {
            return;
        }
        for (SeedBookCategory seed : seedList) {
            Book book = books.get(seed.bookId());
            Category category = categories.get(seed.categoryId());
            if (book == null || category == null) {
                log.warn("Missing book/category when seeding book category link (bookId={}, categoryId={})", seed.bookId(), seed.categoryId());
                continue;
            }
            BookCategoryId id = new BookCategoryId(book.getId(), category.getId());
            if (bookCategoryRepository.existsById(id)) {
                continue;
            }
            BookCategory link = BookCategory.builder()
                    .book(book)
                    .category(category)
                    .priority(seed.priority())
                    .createdAt(seed.createdAt())
                    .build();
            bookCategoryRepository.save(link);
        }
    }

    private Map<Long, ShoppingCart> seedCarts(List<SeedCart> cartSeed, Map<Long, User> users) {
        Map<Long, ShoppingCart> map = new HashMap<>();
        if (cartSeed == null) {
            return map;
        }
        for (SeedCart seed : cartSeed) {
            User user = users.get(seed.userId());
            if (user == null) {
                log.warn("User id {} not found while seeding cart", seed.userId());
                continue;
            }
            ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), seed.status() != null ? seed.status() : "ACTIVE")
                    .orElseGet(() -> ShoppingCart.builder().user(user).status(seed.status() != null ? seed.status() : "ACTIVE").build());
            cart.setUser(user);
            cart.setStatus(seed.status() != null ? seed.status() : "ACTIVE");
            cart.setTotalItems(seed.totalItems() != null ? seed.totalItems() : 0);
            cart.setSubtotal(seed.subtotal() != null ? seed.subtotal() : BigDecimal.ZERO);
            ShoppingCart saved = shoppingCartRepository.save(cart);
            if (seed.id() != null) {
                map.put(seed.id(), saved);
            }
        }
        return map;
    }

    private void seedCartItems(List<SeedCartItem> itemSeed, Map<Long, ShoppingCart> carts, Map<Long, ProductSku> skus) {
        if (itemSeed == null) {
            return;
        }
        for (SeedCartItem seed : itemSeed) {
            ShoppingCart cart = carts.get(seed.cartId());
            ProductSku sku = skus.get(seed.skuId());
            if (cart == null || sku == null) {
                log.warn("Missing cart or sku for cart item seed (cartId={}, skuId={})", seed.cartId(), seed.skuId());
                continue;
            }
            CartItem cartItem = cartItemRepository.findByCartAndProductSku(cart, sku)
                    .orElseGet(() -> CartItem.builder().cart(cart).productSku(sku).build());
            cartItem.setCart(cart);
            cartItem.setProductSku(sku);
            cartItem.setQuantity(seed.quantity() != null ? seed.quantity() : 1);
            cartItem.setUnitPrice(seed.unitPrice() != null ? seed.unitPrice() : sku.resolveCurrentPrice());
            cartItemRepository.save(cartItem);
        }
    }

    private Map<Long, Order> seedOrders(List<SeedOrder> orderSeed, Map<Long, User> users, Map<Long, ShoppingCart> carts) {
        Map<Long, Order> map = new HashMap<>();
        if (orderSeed == null) {
            return map;
        }
        for (SeedOrder seed : orderSeed) {
            User user = users.get(seed.userId());
            ShoppingCart cart = seed.cartId() != null ? carts.get(seed.cartId()) : null;
            if (user == null) {
                log.warn("User id {} not found while seeding order", seed.userId());
                continue;
            }
            Optional<Order> optional = orderRepository.findByUserAndStatus(user, seed.status());
            Order order = optional.orElseGet(() -> Order.builder().user(user).status(seed.status()).build());
            order.setUser(user);
            order.setCart(cart);
            order.setShippingAddressId(seed.shippingAddressId());
            order.setBillingAddressId(seed.billingAddressId());
            order.setTotalAmount(seed.totalAmount() != null ? seed.totalAmount() : BigDecimal.ZERO);
            order.setCurrency(seed.currency());
            order.setStatus(seed.status());
            order.setCouponId(seed.couponId());
            order.setPlacedAt(seed.placedAt() != null ? seed.placedAt().toLocalDateTime() : null);
            Order saved = orderRepository.save(order);
            if (seed.id() != null) {
                map.put(seed.id(), saved);
            }
        }
        return map;
    }

    private void seedOrderItems(List<SeedOrderItem> itemSeed, Map<Long, Order> orders, Map<Long, Book> books, Map<Long, ProductSku> skus) {
        if (itemSeed == null) {
            return;
        }
        for (SeedOrderItem seed : itemSeed) {
            Order order = orders.get(seed.orderId());
            Book book = books.get(seed.bookId());
            ProductSku sku = skus.get(seed.skuId());
            if (order == null || book == null || sku == null) {
                log.warn("Missing order/book/sku for order item seed (orderId={}, bookId={}, skuId={})", seed.orderId(), seed.bookId(), seed.skuId());
                continue;
            }
            OrderItem orderItem = orderItemRepository.findByOrderAndProductSku(order, sku)
                    .orElseGet(() -> OrderItem.builder().order(order).productSku(sku).book(book).build());
            orderItem.setOrder(order);
            orderItem.setBook(book);
            orderItem.setProductSku(sku);
            orderItem.setQuantity(seed.quantity() != null ? seed.quantity() : 1);
            orderItem.setUnitPrice(seed.unitPrice() != null ? seed.unitPrice() : sku.resolveCurrentPrice());
            orderItem.setTaxAmount(seed.taxAmount());
            orderItem.setDiscountAmount(seed.discountAmount());
            orderItemRepository.save(orderItem);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedData(
            List<SeedRole> roles,
            List<SeedUser> users,
            List<SeedCategory> categories,
            List<SeedBook> books,
            List<SeedProductSku> productSkus,
            List<SeedInventory> inventory,
            List<SeedBookCategory> bookCategories,
            List<SeedCart> shoppingCarts,
            List<SeedCartItem> cartItems,
            List<SeedOrder> orders,
            List<SeedOrderItem> orderItems) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedRole(Long id, String name, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedUser(Long id, String username, String email, String passwordHash, Long roleId, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedCategory(Long id, String name, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedBook(Long id, String title, String subtitle, String description, String language, Integer pages,
            Long publisherId, LocalDate publishedDate, BigDecimal basePrice, Long defaultSkuId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedProductSku(Long id, Long bookId, String sku, String format, BigDecimal priceOverride,
            Integer weightGrams, Integer lengthMm, Integer widthMm, Integer heightMm) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedInventory(Long skuId, Integer stock, Integer reserved) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedBookCategory(Long bookId, Long categoryId, Integer priority, LocalDateTime createdAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedCart(Long id, Long userId, String status, Integer totalItems, BigDecimal subtotal) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedCartItem(Long id, Long cartId, Long skuId, Integer quantity, BigDecimal unitPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
        private record SeedOrder(Long id, Long userId, Long cartId, Long shippingAddressId, Long billingAddressId,
            BigDecimal totalAmount, String currency, String status, Long couponId, OffsetDateTime placedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedOrderItem(Long id, Long orderId, Long skuId, Long bookId, Integer quantity, BigDecimal unitPrice,
            BigDecimal taxAmount, BigDecimal discountAmount) {
    }
}
