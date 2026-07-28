package com.travelassistant.integration;

import com.travelassistant.TestFixtures;
import com.travelassistant.dto.ApiDtos.ExternalFraudResult;
import com.travelassistant.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class MockFraudDetectionClientAtmTest {
    private final MockFraudDetectionClient client = new MockFraudDetectionClient();

    @Test
    void addsAtmSignalForAtmWithdrawal() {
        TravelTransaction atm = TestFixtures.atmWithdrawal("txn-atm", java.time.Instant.now());
        ExternalFraudResult result = client.score(atm, TestFixtures.fraudContext(
                TestFixtures.trip("trip-1", "customer-001", "Japan", "Tokyo", Enums.TripStatus.ACTIVE),
                TestFixtures.card("card-001", "customer-001", new BigDecimal("500")),
                java.util.List.of()));

        assertThat(result.riskScore()).isGreaterThanOrEqualTo(10);
        assertThat(result.signals()).contains("ATM");
    }

    @Test
    void doesNotAddAtmSignalForPurchase() {
        TravelTransaction purchase = TravelTransaction.builder()
                .transactionId("txn-purchase").transactionType(Enums.TransactionType.PURCHASE)
                .billingAmount(new BigDecimal("50")).merchantCountry("Japan").build();
        ExternalFraudResult result = client.score(purchase, TestFixtures.fraudContext(
                TestFixtures.trip("trip-1", "customer-001", "Japan", "Tokyo", Enums.TripStatus.ACTIVE),
                TestFixtures.card("card-001", "customer-001", new BigDecimal("500")),
                java.util.List.of()));

        assertThat(result.signals()).doesNotContain("ATM");
    }
}
