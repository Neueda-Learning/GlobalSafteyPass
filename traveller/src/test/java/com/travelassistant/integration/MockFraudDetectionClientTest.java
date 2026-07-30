package com.travelassistant.integration;

import com.travelassistant.model.Enums;
import com.travelassistant.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

class MockFraudDetectionClientTest {
    private final MockFraudDetectionClient client = new MockFraudDetectionClient();

    @Test
    void highAmountAndDestinationMismatchIncreaseScore() {
        var trip = TestDataFactory.trip("trip-1", "customer-001", "Japan",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("account-001", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("txn-1", "France", new BigDecimal("1500"), Enums.TransactionStatus.APPROVED);
        var ctx = TestDataFactory.fraudContext(trip, card, account, java.util.List.of());
        var result = client.score(txn, ctx);
        org.assertj.core.api.Assertions.assertThat(result.riskScore()).isGreaterThanOrEqualTo(60);
        org.assertj.core.api.Assertions.assertThat(result.signals()).contains("HIGH_AMOUNT", "DESTINATION_MISMATCH");
    }
}
