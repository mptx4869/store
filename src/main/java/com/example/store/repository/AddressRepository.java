package com.example.store.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserId(Long userId);
}
