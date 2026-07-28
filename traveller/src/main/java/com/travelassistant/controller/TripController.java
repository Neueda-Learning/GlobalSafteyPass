package com.travelassistant.controller;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.TripService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/travel/trips") @Tag(name="Trips",description="Travel plan management")
public class TripController {
    private final TripService service;private final CustomerContext customer;
    public TripController(TripService s,CustomerContext c){service=s;customer=c;}
    @PostMapping @Operation(summary="Create a trip") public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(customer.customerId(),r));}
    @GetMapping @Operation(summary="List trips") public ListResponse<TripResponse> list(){var d=service.list(customer.customerId());return new ListResponse<>(d,d.size());}
    @GetMapping("/{id}") @Operation(summary="Get a trip") public TripResponse get(@PathVariable String id){return service.get(customer.customerId(),id);}
    @PutMapping("/{id}") @Operation(summary="Update a trip") public TripResponse update(@PathVariable String id,@Valid @RequestBody TripRequest r){return service.update(customer.customerId(),id,r);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Cancel/delete a trip") public void delete(@PathVariable String id){service.delete(customer.customerId(),id);}
    @PostMapping("/{id}/cash-exchange-plan") @Operation(summary="Record a plan to exchange cash on arrival") public TripResponse cashExchange(@PathVariable String id){return service.planCashExchange(customer.customerId(),id);}
}
