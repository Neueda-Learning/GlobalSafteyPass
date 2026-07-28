package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.exception.*;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import com.travelassistant.rule.fraud.FraudContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class TransactionService {
    private final TransactionRepository txs;private final TripRepository trips;private final CardRepository cards;private final AccountRepository accounts;
    private final ExchangeRateService fx;private final FraudMonitoringService fraud;private final AuditService audit;private final PaymentFailureService failures;
    public TransactionService(TransactionRepository txs,TripRepository trips,CardRepository cards,AccountRepository accounts,ExchangeRateService fx,FraudMonitoringService fraud,AuditService audit,PaymentFailureService failures){
        this.txs=txs;this.trips=trips;this.cards=cards;this.accounts=accounts;this.fx=fx;this.fraud=fraud;this.audit=audit;this.failures=failures;}
    @Transactional public TransactionResponse receive(String customer,TransactionEventRequest r){
        if(txs.existsById(r.transactionId()))throw new DuplicateTransactionException("Transaction ID already processed.");
        if(r.customerId()!=null&&!r.customerId().equals(customer))throw new ForbiddenException("Event customer does not match current customer.");
        Card card=cards.findById(r.cardId()).orElseThrow(()->new ResourceNotFoundException("Card not found."));
        if(!card.getCustomerId().equals(customer))throw new ForbiddenException("Card does not belong to current customer.");
        LocalDate date=r.transactionTime().atZone(ZoneOffset.UTC).toLocalDate();
        Trip trip=trips.findByCustomerIdOrderByStartDateDesc(customer).stream().filter(t->t.getPreferredCardId().equals(r.cardId())&&!date.isBefore(t.getStartDate())&&!date.isAfter(t.getEndDate())).findFirst()
                .orElseGet(()->trips.findByCustomerIdOrderByStartDateDesc(customer).stream().filter(t->t.getPreferredCardId().equals(r.cardId())).findFirst().orElse(null));
        String target=trip==null?(r.billingCurrency()==null?r.originalCurrency():r.billingCurrency()):trip.getBudgetCurrency();
        ExchangeRateQuote quote=fx.rate(r.originalCurrency(),target,date);
        BigDecimal estimated=r.originalAmount().multiply(quote.rate()).setScale(2,RoundingMode.HALF_UP);
        TravelTransaction t=TravelTransaction.builder().transactionId(r.transactionId()).customerId(customer).tripId(trip==null?null:trip.getId()).cardId(r.cardId())
                .merchantName(r.merchantName()).merchantCountry(r.merchantCountry()).merchantCity(r.merchantCity()).merchantCategory(r.merchantCategory())
                .originalAmount(r.originalAmount()).originalCurrency(r.originalCurrency().toUpperCase())
                .billingAmount(r.billingAmount()==null?estimated:r.billingAmount()).billingCurrency(r.billingCurrency()==null?target:r.billingCurrency())
                .exchangeRate(quote.rate()).transactionTime(r.transactionTime()).transactionType(r.transactionType()).status(r.status())
                .failureCode(r.failureCode()).recoveryStatus(r.status()==Enums.TransactionStatus.DECLINED?Enums.RecoveryStatus.PAYMENT_INTERRUPTED:null)
                .recoveryUpdatedAt(r.status()==Enums.TransactionStatus.DECLINED?Instant.now():null).createdAt(Instant.now()).build();
        txs.save(t);audit.log(customer,"TRANSACTION_RECEIVED","TRANSACTION",t.getTransactionId(),"status="+t.getStatus());
        List<TravelTransaction> recent=txs.findByCardIdAndTransactionTimeBetween(card.getId(),r.transactionTime().minus(Duration.ofDays(7)),r.transactionTime().plusSeconds(1));
        List<TravelTransaction> history=txs.findByCustomerIdOrderByTransactionTimeDesc(customer).stream()
                .filter(x->!x.getTransactionId().equals(t.getTransactionId())).toList();
        List<TravelTransaction> approvedHistory=history.stream()
                .filter(x->x.getStatus()==Enums.TransactionStatus.APPROVED&&x.getBillingAmount()!=null).toList();
        BigDecimal average=approvedHistory.stream().map(TravelTransaction::getBillingAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1,approvedHistory.size())),2,RoundingMode.HALF_UP);
        Account account=accounts.findById(card.getLinkedAccountId()).orElseThrow();
        fraud.evaluate(t,new FraudContext(trip,recent,card,account,average,
                history.stream().map(TravelTransaction::getMerchantCountry).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet()),
                history.stream().map(TravelTransaction::getMerchantCategory).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet())));
        return map(t);
    }
    public FailureExplanation failure(String customer,String id){TravelTransaction t=owned(customer,id);if(t.getStatus()!=Enums.TransactionStatus.DECLINED)throw new IllegalArgumentException("Transaction was not declined.");return failures.explain(id,t.getFailureCode());}
    public List<TransactionResponse> list(String customer){return txs.findByCustomerIdOrderByTransactionTimeDesc(customer).stream().map(this::map).toList();}
    public TravelTransaction owned(String customer,String id){TravelTransaction t=txs.findById(id).orElseThrow(()->new ResourceNotFoundException("Transaction not found."));if(!t.getCustomerId().equals(customer))throw new ForbiddenException("Transaction does not belong to current customer.");return t;}
    public TransactionResponse map(TravelTransaction t){return new TransactionResponse(t.getTransactionId(),t.getTripId(),t.getMerchantName(),t.getMerchantCountry(),t.getMerchantCategory(),t.getOriginalAmount(),t.getOriginalCurrency(),t.getBillingAmount(),t.getBillingCurrency(),t.getExchangeRate(),t.getTransactionTime(),t.getTransactionType(),t.getStatus(),t.getFailureCode(),t.getRecoveryStatus());}
}
