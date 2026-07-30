package com.travelassistant.repository;

import com.travelassistant.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer,String> {
    Optional<Customer> findByDisplayNameIgnoreCase(String displayName);
}
