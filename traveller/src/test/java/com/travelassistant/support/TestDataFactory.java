package com.travelassistant.support;

import com.travelassistant.model.*;
import com.travelassistant.rule.fraud.FraudContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class TestDataFactory {
    private TestDataFactory() {}

    public static Trip trip(String id, String customer, String country, LocalDate start, LocalDate end) {
        return Trip.builder().id(id).customerId(customer).destinationCountry(country).destinationCity("City")
                .startDate(start).endDate(end).budget(new BigDecimal("2000")).budgetCurrency("USD")
                .preferredCardId("card-001").status(Enums.TripStatus.PLANNED).build();
    }

    public static Card card(String id, String customer, Enums.CardStatus status) {
        return Card.builder().id(id).customerId(customer).maskedCardNumber("**** 1234").cardType("VISA")
                .expiryMonth(12).expiryYear(2029).status(status).overseasPaymentsEnabled(true)
                .onlinePaymentsEnabled(true).dailyPaymentLimit(new BigDecimal("1500"))
                .dailyWithdrawalLimit(new BigDecimal("500")).linkedAccountId("account-001")
                .mainCurrency("USD").supportedCurrencies("USD,EUR").build();
    }

    public static Account account(String id, BigDecimal balance, Enums.AccountStatus status) {
        return Account.builder().id(id).customerId("customer-001").accountType("CHECKING")
                .currency("USD").availableBalance(balance).status(status).build();
    }

    public static TravelTransaction txn(String id, String country, BigDecimal billing, Enums.TransactionStatus status) {
        return TravelTransaction.builder().transactionId(id).customerId("customer-001").cardId("card-001")
                .merchantName("Shop").merchantCountry(country).merchantCategory("RETAIL")
                .originalAmount(billing).originalCurrency("USD").billingAmount(billing).billingCurrency("USD")
                .transactionTime(Instant.parse("2026-10-02T10:00:00Z")).transactionType(Enums.TransactionType.PURCHASE)
                .status(status).build();
    }

    public static FraudContext fraudContext(Trip trip, Card card, Account account, List<TravelTransaction> recent) {
        return new FraudContext(trip, recent, card, account, new BigDecimal("200"),
                Set.of("Japan", "France"), Set.of("RETAIL", "HOTEL"));
    }
}
