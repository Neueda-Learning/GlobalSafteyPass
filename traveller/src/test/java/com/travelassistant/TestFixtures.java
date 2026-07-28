package com.travelassistant;

import com.travelassistant.model.*;
import com.travelassistant.rule.fraud.FraudContext;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class TestFixtures {
    private TestFixtures() {}

    public static Trip trip(String id, String customer, String country, String city, Enums.TripStatus status) {
        return Trip.builder()
                .id(id).customerId(customer).destinationCountry(country).destinationCity(city)
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 10))
                .budget(new BigDecimal("2000")).budgetCurrency("USD").preferredCardId("card-001")
                .status(status).createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    public static Card card(String id, String customer, BigDecimal withdrawalLimit) {
        return Card.builder()
                .id(id).customerId(customer).maskedCardNumber("**** **** **** 1234")
                .cardType("VISA").expiryMonth(12).expiryYear(2028)
                .status(Enums.CardStatus.ACTIVE).overseasPaymentsEnabled(true).onlinePaymentsEnabled(true)
                .dailyPaymentLimit(new BigDecimal("5000")).dailyWithdrawalLimit(withdrawalLimit)
                .linkedAccountId("acct-001").mainCurrency("USD").supportedCurrencies("USD,EUR,JPY").build();
    }

    public static Account account(String id, String customer) {
        return Account.builder()
                .id(id).customerId(customer).status(Enums.AccountStatus.ACTIVE)
                .availableBalance(new BigDecimal("10000")).currency("USD").build();
    }

    public static TravelTransaction atmWithdrawal(String id, Instant time) {
        return TravelTransaction.builder()
                .transactionId(id).customerId("customer-001").cardId("card-001")
                .merchantName("ATM").merchantCountry("Japan").merchantCategory("ATM")
                .originalAmount(new BigDecimal("200")).originalCurrency("JPY")
                .billingAmount(new BigDecimal("200")).billingCurrency("USD")
                .transactionTime(time).transactionType(Enums.TransactionType.ATM_WITHDRAWAL)
                .status(Enums.TransactionStatus.APPROVED).createdAt(time).build();
    }

    public static FraudContext fraudContext(Trip trip, Card card, List<TravelTransaction> recent) {
        return new FraudContext(trip, recent, card, account("acct-001", "customer-001"),
                new BigDecimal("150"), Set.of("Japan", "France"), Set.of("FOOD", "ATM"));
    }
}
