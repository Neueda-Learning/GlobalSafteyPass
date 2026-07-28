package com.travelassistant;

import com.travelassistant.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class TestFixtures {
    private TestFixtures() {}

    public static Trip trip(String id, String customer) {
        return Trip.builder()
                .id(id).customerId(customer)
                .destinationCountry("Japan").destinationCity("Tokyo")
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 10))
                .budget(new BigDecimal("3000")).budgetCurrency("USD")
                .preferredCardId("card-001").status(Enums.TripStatus.PLANNED)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .cashExchangePlanned(false)
                .build();
    }

    public static Card activeCard(String id, String customer) {
        return Card.builder()
                .id(id).customerId(customer).maskedCardNumber("**** 1234")
                .cardType("VISA").expiryMonth(12).expiryYear(2028)
                .status(Enums.CardStatus.ACTIVE)
                .overseasPaymentsEnabled(true).onlinePaymentsEnabled(true).contactlessEnabled(true)
                .dailyPaymentLimit(new BigDecimal("5000")).dailyWithdrawalLimit(new BigDecimal("1000"))
                .linkedAccountId("acct-001").mainCurrency("USD").supportedCurrencies("USD,JPY,EUR")
                .build();
    }

    public static Account activeAccount(String id, String customer) {
        return Account.builder()
                .id(id).customerId(customer).accountType("CHECKING").currency("USD")
                .availableBalance(new BigDecimal("10000")).status(Enums.AccountStatus.ACTIVE)
                .build();
    }

    public static TravelTransaction purchase(String id, String customer, String tripId) {
        return TravelTransaction.builder()
                .transactionId(id).customerId(customer).tripId(tripId).cardId("card-001")
                .merchantName("Tokyo Store").merchantCountry("Japan").merchantCity("Tokyo")
                .merchantCategory("SHOPPING")
                .originalAmount(new BigDecimal("120")).originalCurrency("JPY")
                .billingAmount(new BigDecimal("120")).billingCurrency("USD")
                .exchangeRate(new BigDecimal("0.0067"))
                .transactionTime(Instant.parse("2026-08-05T10:00:00Z"))
                .transactionType(Enums.TransactionType.PURCHASE)
                .status(Enums.TransactionStatus.APPROVED)
                .recoveryAttemptCount(0).createdAt(Instant.now())
                .build();
    }

    public static FraudAlert openAlert(String id, String customer, String txnId) {
        return FraudAlert.builder()
                .id(id).customerId(customer).transactionId(txnId).cardId("card-001")
                .tripId("trip-1").riskScore(65).riskLevel(Enums.RiskLevel.HIGH)
                .decision(Enums.FraudDecision.REQUIRE_CONFIRMATION)
                .reasonCodes("OUTSIDE_DESTINATION").customerMessage("Unusual activity")
                .status(Enums.AlertStatus.OPEN).createdAt(Instant.now())
                .customerResponse(Enums.CustomerResponse.NONE)
                .build();
    }
}
