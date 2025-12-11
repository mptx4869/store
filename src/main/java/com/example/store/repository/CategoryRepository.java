package com.example.store.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.example.store.model.Category;

public interface CategoryRepository extends Repository<Category, Long> {

    Category save(Category category);

    Category findByName(String name);

    List<Category> findAll();
}
