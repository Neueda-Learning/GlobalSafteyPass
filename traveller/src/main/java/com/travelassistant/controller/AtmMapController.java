package com.travelassistant.controller;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.AtmMapService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/travel/maps") @Tag(name="ATM map")
public class AtmMapController {
    private final AtmMapService maps;private final CustomerContext customer;
    public AtmMapController(AtmMapService maps,CustomerContext customer){this.maps=maps;this.customer=customer;}
    @GetMapping("/geocode") public MapLocationResponse geocode(@RequestParam String q){
        customer.customerId();return maps.geocode(q);
    }
    @GetMapping("/atms") public AtmSearchResponse atms(@RequestParam double lat,@RequestParam double lon,
            @RequestParam(defaultValue="2000")int radius){
        customer.customerId();return maps.nearby(lat,lon,radius);
    }
}
