package com.travelassistant.controller;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/travel/trips") @Tag(name="Trips",description="Travel plan management")
public class TripController {
    private final TripService service;private final CashExchangePlanningService cashPlanning;private final CustomerContext customer;
    public TripController(TripService s,CashExchangePlanningService cashPlanning,CustomerContext c){service=s;this.cashPlanning=cashPlanning;customer=c;}
    @PostMapping @Operation(summary="Create a trip") public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(customer.customerId(),r));}
    @GetMapping @Operation(summary="List trips") public ListResponse<TripResponse> list(){var d=service.list(customer.customerId());return new ListResponse<>(d,d.size());}
    @GetMapping("/{id}") @Operation(summary="Get a trip") public TripResponse get(@PathVariable String id){return service.get(customer.customerId(),id);}
    @PutMapping("/{id}") @Operation(summary="Update a trip") public TripResponse update(@PathVariable String id,@Valid @RequestBody TripRequest r){return service.update(customer.customerId(),id,r);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Cancel/delete a trip") public void delete(@PathVariable String id){service.delete(customer.customerId(),id);}
    @PostMapping("/{id}/cash-exchange-plan") @Operation(summary="Save a planned backup cash option")
    public CashExchangePlanResponse cashExchange(@PathVariable String id,@Valid @RequestBody CashExchangePlanRequest request){
        return cashPlanning.save(customer.customerId(),id,request);
    }
}
