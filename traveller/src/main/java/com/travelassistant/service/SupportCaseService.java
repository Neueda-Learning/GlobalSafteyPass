package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.SupportCaseResponse;
import com.travelassistant.model.*;
import com.travelassistant.repository.SupportCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class SupportCaseService {
    private final SupportCaseRepository cases;
    private final TransactionService transactions;
    public SupportCaseService(SupportCaseRepository cases,TransactionService transactions){this.cases=cases;this.transactions=transactions;}
    public List<SupportCaseResponse> list(String customer){return cases.findByCustomerIdOrderByUpdatedAtDesc(customer).stream().map(this::map).toList();}
    @Transactional public SupportCaseResponse openPaymentCase(String customer,String transactionId){
        TravelTransaction transaction=transactions.owned(customer,transactionId);
        SupportCase support=cases.findFirstByCustomerIdAndTransactionIdOrderByCreatedAtDesc(customer,transactionId)
                .filter(c->c.getStatus()!=Enums.CaseStatus.RESOLVED)
                .orElseGet(()->SupportCase.builder().id("CASE-"+UUID.randomUUID().toString().substring(0,8).toUpperCase())
                        .customerId(customer).tripId(transaction.getTripId()).transactionId(transactionId)
                        .type(Enums.CaseType.PAYMENT_SUPPORT).status(Enums.CaseStatus.SUBMITTED)
                        .title("Payment review · "+transaction.getMerchantName())
                        .currentUpdate("Request received. A payments specialist will review the decline context.")
                        .createdAt(Instant.now()).updatedAt(Instant.now()).build());
        return map(cases.save(support));
    }
    @Transactional public SupportCaseResponse openFraudCase(String customer,TravelTransaction transaction){
        SupportCase support=cases.findFirstByCustomerIdAndTransactionIdOrderByCreatedAtDesc(customer,transaction.getTransactionId())
                .orElseGet(()->SupportCase.builder().id(transaction.getFraudCaseReference()).customerId(customer)
                        .tripId(transaction.getTripId()).transactionId(transaction.getTransactionId())
                        .type(Enums.CaseType.FRAUD_INVESTIGATION).status(Enums.CaseStatus.UNDER_REVIEW)
                        .title("Fraud investigation · "+transaction.getMerchantName())
                        .currentUpdate("The card payment is disputed and the investigation team is reviewing it.")
                        .createdAt(Instant.now()).updatedAt(Instant.now()).build());
        return map(cases.save(support));
    }
    public SupportCaseResponse map(SupportCase c){return new SupportCaseResponse(c.getId(),c.getTripId(),c.getTransactionId(),c.getType(),c.getStatus(),c.getTitle(),c.getCurrentUpdate(),c.getCreatedAt(),c.getUpdatedAt());}
}
