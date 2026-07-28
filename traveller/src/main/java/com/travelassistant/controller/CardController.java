package com.travelassistant.controller;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.CardControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.travelassistant.security.AuthenticationService;
@RestController @RequestMapping("/api/travel/cards") @Tag(name="Card controls")
public class CardController {
    private final CardControlService service;private final CustomerContext customer;private final AuthenticationService authentication;
    public CardController(CardControlService s,CustomerContext c,AuthenticationService authentication){service=s;customer=c;this.authentication=authentication;}
    @GetMapping @Operation(summary="List cards") public ListResponse<CardResponse> list(){var d=service.list(customer.customerId());return new ListResponse<>(d,d.size());}
    @PostMapping("/{id}/enable-overseas-payments") public CardResponse enable(@PathVariable String id){return service.overseas(customer.customerId(),id,true);}
    @PostMapping("/{id}/disable-overseas-payments") public CardResponse disable(@PathVariable String id){return service.overseas(customer.customerId(),id,false);}
    @PostMapping("/{id}/enable-online-payments") public CardResponse enableOnline(@PathVariable String id){return service.online(customer.customerId(),id,true);}
    @PostMapping("/{id}/freeze") public CardResponse freeze(@PathVariable String id,@RequestHeader(value="X-Step-Up-Token",required=false)String token){String c=customer.customerId();authentication.consumeStepUp(token,c,"CARD_FREEZE",id);return service.freeze(c,id);}
    @PostMapping("/{id}/unfreeze") public CardResponse unfreeze(@PathVariable String id,@RequestHeader(value="X-Step-Up-Token",required=false)String token){String c=customer.customerId();authentication.consumeStepUp(token,c,"CARD_UNFREEZE",id);return service.unfreeze(c,id);}
    @PutMapping("/{id}/payment-limit") public CardResponse payment(@PathVariable String id,@RequestHeader(value="X-Step-Up-Token",required=false)String token,@Valid @RequestBody LimitRequest r){String c=customer.customerId();authentication.consumeStepUp(token,c,"PAYMENT_LIMIT_CHANGE",id);return service.paymentLimit(c,id,r.newLimit());}
    @PutMapping("/{id}/withdrawal-limit") public CardResponse withdrawal(@PathVariable String id,@RequestHeader(value="X-Step-Up-Token",required=false)String token,@Valid @RequestBody LimitRequest r){String c=customer.customerId();authentication.consumeStepUp(token,c,"WITHDRAWAL_LIMIT_CHANGE",id);return service.withdrawalLimit(c,id,r.newLimit());}
}
