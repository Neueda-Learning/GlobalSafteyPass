package com.travelassistant.controller;

import com.travelassistant.exception.ResourceNotFoundException;
import com.travelassistant.repository.CustomerRepository;
import com.travelassistant.security.CustomerContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {
    private final CustomerRepository customers;
    private final CustomerContext customer;

    public CustomerController(CustomerRepository customers,CustomerContext customer){
        this.customers=customers;this.customer=customer;
    }

    @GetMapping("/profile")
    public ProfileResponse profile(){
        var profile=customers.findById(customer.customerId())
                .orElseThrow(()->new ResourceNotFoundException("Customer profile not found."));
        return new ProfileResponse(profile.getId(),profile.getDisplayName());
    }

    public record ProfileResponse(String customerId,String displayName){}
}
