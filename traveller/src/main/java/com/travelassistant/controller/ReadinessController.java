package com.travelassistant.controller;
import com.travelassistant.dto.ApiDtos.CurrencyFallbackRequest;
import com.travelassistant.dto.ApiDtos.ReadinessResponse;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.ReadinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/travel/trips/{tripId}") @Tag(name="Travel readiness")
public class ReadinessController {
    private final ReadinessService service;private final CustomerContext customer;
    public ReadinessController(ReadinessService s,CustomerContext c){service=s;customer=c;}
    @PostMapping("/readiness-check") @Operation(summary="Run all readiness rules") public ReadinessResponse check(@PathVariable String tripId){return service.check(customer.customerId(),tripId);}
    @GetMapping("/readiness") @Operation(summary="Get latest readiness result") public ReadinessResponse get(@PathVariable String tripId){return service.get(customer.customerId(),tripId);}
    @PostMapping("/readiness/currency-fallback")
    @Operation(summary="Record how to handle unsupported destination currency")
    public void currencyFallback(@PathVariable String tripId,@Valid @RequestBody CurrencyFallbackRequest request){
        service.setCurrencyFallback(customer.customerId(),tripId,request.option());
    }
}
