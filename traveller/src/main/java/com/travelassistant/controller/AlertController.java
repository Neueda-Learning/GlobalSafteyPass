package com.travelassistant.controller;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.AlertService;
import com.travelassistant.security.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/travel/alerts") @Tag(name="Fraud alerts")
public class AlertController {
    private final AlertService service;private final CustomerContext customer;private final AuthenticationService authentication;
    public AlertController(AlertService s,CustomerContext c,AuthenticationService authentication){service=s;customer=c;this.authentication=authentication;}
    @GetMapping public ListResponse<AlertResponse> list(){var d=service.list(customer.customerId());return new ListResponse<>(d,d.size());}
    @GetMapping("/{id}") public AlertResponse get(@PathVariable String id){return service.get(customer.customerId(),id);}
    @PostMapping("/{id}/confirm") @Operation(summary="Confirm transaction is safe") public AlertResponse confirm(@PathVariable String id){return service.confirm(customer.customerId(),id);}
    @PostMapping("/{id}/report") @Operation(summary="Report transaction as fraud") public AlertResponse report(@PathVariable String id){return service.report(customer.customerId(),id);}
    @PostMapping("/{id}/freeze-card") @Operation(summary="Freeze the affected card") public AlertResponse freeze(@PathVariable String id,
            @RequestHeader(value="X-Step-Up-Token",required=false)String token){String c=customer.customerId();AlertResponse alert=service.get(c,id);authentication.consumeStepUp(token,c,"CARD_FREEZE",alert.cardId());return service.freeze(c,id);}
}
