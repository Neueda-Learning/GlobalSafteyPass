package com.travelassistant.controller;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.time.*;

@RestController
@RequestMapping("/api/travel/exchange-rates")
@Tag(name="Exchange rates",description="Live indicative travel exchange rates")
public class ExchangeRateController {
    private final ExchangeRateService service; private final CustomerContext customer;
    public ExchangeRateController(ExchangeRateService service,CustomerContext customer){this.service=service;this.customer=customer;}
    @GetMapping("/live")
    @Operation(summary="Get a live indicative exchange rate")
    public ResponseEntity<LiveRateResponse> live(@RequestParam(defaultValue="USD")String base,@RequestParam(defaultValue="JPY")String quote){
        customer.customerId();ExchangeRateQuote q=service.rate(base,quote,LocalDate.now());
        LiveRateResponse body=new LiveRateResponse(q.sourceCurrency(),q.targetCurrency(),q.rate(),q.rateDate(),q.provider(),q.estimated(),Instant.now(),
                "Indicative rate for travel budgeting only. Card settlement uses the bank-posted billing amount.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
