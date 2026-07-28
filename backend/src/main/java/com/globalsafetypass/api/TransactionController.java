package com.globalsafetypass.api;

import com.globalsafetypass.api.ApiDtos.*;
import com.globalsafetypass.model.BankTransaction;
import com.globalsafetypass.repository.TransactionRepository;
import com.globalsafetypass.service.TransactionIngestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TransactionController {
    private final TransactionRepository transactions;
    private final TransactionIngestionService ingestion;
    public TransactionController(TransactionRepository transactions,
                                 TransactionIngestionService ingestion) {
        this.transactions = transactions; this.ingestion = ingestion;
    }

    @GetMapping("/transactions")
    public List<BankTransaction> list() { return transactions.findTop50ByOrderByOccurredAtDesc(); }

    @PostMapping("/bank/events")
    public IngestionResult bankEvent(@Valid @RequestBody BankEvent event) {
        return ingestion.ingest(event);
    }

    @PostMapping("/demo/payment")
    public IngestionResult demoPayment(@Valid @RequestBody BankEvent event) {
        return ingestion.ingest(event);
    }
}

