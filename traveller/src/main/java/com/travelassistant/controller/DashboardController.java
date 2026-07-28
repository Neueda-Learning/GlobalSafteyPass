package com.travelassistant.controller;
import com.travelassistant.dto.ApiDtos.DashboardResponse;
import com.travelassistant.dto.ApiDtos.JourneyExchangeRateResponse;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.TravelDashboardService;
import com.travelassistant.service.JourneyExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/travel/trips") @Tag(name="Dashboard")
public class DashboardController {
    private final TravelDashboardService service;private final JourneyExchangeRateService rates;private final CustomerContext customer;
    public DashboardController(TravelDashboardService s,JourneyExchangeRateService rates,CustomerContext c){service=s;this.rates=rates;customer=c;}
    @GetMapping("/{id}/dashboard") @Operation(summary="Get spending and safety dashboard") public DashboardResponse dashboard(@PathVariable String id){return service.get(customer.customerId(),id);}
    @GetMapping("/{id}/exchange-rate") @Operation(summary="Get preferred-card to destination exchange rate") public JourneyExchangeRateResponse exchangeRate(@PathVariable String id){return rates.get(customer.customerId(),id);}
}
