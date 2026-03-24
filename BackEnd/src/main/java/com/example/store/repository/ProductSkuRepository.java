package com.example.store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.ProductSku;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    Optional<ProductSku> findBySku(String sku);

    Optional<ProductSku> findByBookIdAndId(Long bookId, Long id);

    Optional<ProductSku> findFirstByBookId(Long bookId);

    List<ProductSku> findByBookId(Long bookId);
}
