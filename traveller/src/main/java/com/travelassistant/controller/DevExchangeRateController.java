package com.travelassistant.controller;

import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import com.travelassistant.dto.ApiDtos.LiveRateResponse;
import com.travelassistant.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/public/exchange-rates")
@Tag(name = "Exchange rates (public)", description = "Public live exchange-rate endpoint for quick health checks")
public class DevExchangeRateController {
    private final ExchangeRateService service;

    public DevExchangeRateController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping("/live")
    @Operation(summary = "Get live indicative exchange rate without authentication")
    public ResponseEntity<LiveRateResponse> live(@RequestParam(defaultValue = "USD") String base,
                                                 @RequestParam(defaultValue = "JPY") String quote) {
        ExchangeRateQuote q = service.rate(base, quote, LocalDate.now());
        LiveRateResponse body = new LiveRateResponse(
                q.sourceCurrency(), q.targetCurrency(), q.rate(), q.rateDate(), q.provider(), q.estimated(), Instant.now(),
                "Public endpoint for integration checks. Indicative rate for travel budgeting only."
        );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
