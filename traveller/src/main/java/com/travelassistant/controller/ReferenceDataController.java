package com.travelassistant.controller;

import com.travelassistant.dto.ApiDtos.CityOption;
import com.travelassistant.dto.ApiDtos.CountryOption;
import com.travelassistant.dto.ApiDtos.CurrencyOption;
import com.travelassistant.dto.ApiDtos.ListResponse;
import com.travelassistant.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/reference")
@Tag(name = "Reference data", description = "Country, city, and currency lookup data")
public class ReferenceDataController {
    private final ReferenceDataService service;

    public ReferenceDataController(ReferenceDataService service) {
        this.service = service;
    }

    @GetMapping("/countries")
    @Operation(summary = "Get all countries with ISO codes and mainstream currency")
    public ListResponse<CountryOption> countries() {
        List<CountryOption> data = service.countries();
        return new ListResponse<>(data, data.size());
    }

    @GetMapping("/cities")
    @Operation(summary = "Get cities by selected country")
    public ListResponse<CityOption> cities(@RequestParam String country) {
        List<CityOption> data = service.cities(country);
        return new ListResponse<>(data, data.size());
    }

    @GetMapping("/currencies")
    @Operation(summary = "Get Frankfurter-supported currencies, mainstream currency first for selected country")
    public ListResponse<CurrencyOption> currencies(@RequestParam(required = false) String country) {
        List<CurrencyOption> data = service.currencies(country);
        return new ListResponse<>(data, data.size());
    }
}
