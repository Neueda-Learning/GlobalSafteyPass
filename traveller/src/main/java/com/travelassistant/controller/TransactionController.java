package com.travelassistant.controller;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/travel/transactions") @Tag(name="Transactions")
public class TransactionController {
    private final TransactionService service;private final com.travelassistant.service.SupportCaseService cases;private final com.travelassistant.service.PaymentRecoveryService recovery;private final CustomerContext customer;
    public TransactionController(TransactionService s,com.travelassistant.service.SupportCaseService cases,com.travelassistant.service.PaymentRecoveryService recovery,CustomerContext c){service=s;this.cases=cases;this.recovery=recovery;customer=c;}
    @GetMapping @Operation(summary="List all customer travel transactions") public ListResponse<TransactionResponse> list(){var d=service.list(customer.customerId());return new ListResponse<>(d,d.size());}
    @PostMapping("/events") @Operation(summary="Receive an idempotent transaction event") public ResponseEntity<TransactionResponse> receive(@Valid @RequestBody TransactionEventRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.receive(customer.customerId(),r));}
    @GetMapping("/{id}/failure-explanation") @Operation(summary="Explain a declined payment") public FailureExplanation failure(@PathVariable String id){return service.failure(customer.customerId(),id);}
    @PostMapping("/{id}/support-case") @Operation(summary="Open or return a tracked payment support case") public SupportCaseResponse support(@PathVariable String id){return cases.openPaymentCase(customer.customerId(),id);}
    @GetMapping("/{id}/recovery") @Operation(summary="Explain a failed payment and recommend the best recovery action") public PaymentRecoveryResponse recovery(@PathVariable String id){return recovery.get(customer.customerId(),id);}
    @PostMapping("/{id}/recovery/retry") @Operation(summary="Retry authorization without creating a duplicate transaction") public PaymentRecoveryResponse retry(@PathVariable String id){return recovery.retry(customer.customerId(),id);}
    @PostMapping("/{id}/recovery/use-card") @Operation(summary="Complete the original payment with an eligible alternate card") public PaymentRecoveryResponse useCard(@PathVariable String id,@Valid @RequestBody AlternateCardRecoveryRequest request){return recovery.useAlternateCard(customer.customerId(),id,request.cardId());}
}
