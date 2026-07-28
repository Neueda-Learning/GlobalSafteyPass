package com.travelassistant.integration;

import com.travelassistant.TestFixtures;
import com.travelassistant.rule.fraud.FraudContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mock Integrations")
class MockIntegrationTest {
    private final MockExchangeRateProvider fx = new MockExchangeRateProvider();
    private final MockFraudDetectionClient fraud = new MockFraudDetectionClient();

    @Test @DisplayName("mock FX JPY to USD")
    void mockFxJpyToUsd() {
        var quote = fx.getRate("JPY", "USD", java.time.LocalDate.now());
        assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("0.0067"));
        assertThat(quote.estimated()).isTrue();
    }

    @Test @DisplayName("mock FX same currency")
    void mockFxSameCurrency() {
        assertThat(fx.getRate("USD", "USD", java.time.LocalDate.now()).rate()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test @DisplayName("mock FX EUR to USD")
    void mockFxEurToUsd() {
        assertThat(fx.getRate("EUR", "USD", java.time.LocalDate.now()).rate()).isEqualByComparingTo(new BigDecimal("1.09"));
    }

    @Test @DisplayName("mock fraud high amount")
    void mockFraudHighAmount() {
        var txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setBillingAmount(new BigDecimal("2000"));
        var ctx = new FraudContext(null, List.of(), TestFixtures.activeCard("c", "customer-001"),
                TestFixtures.activeAccount("a", "customer-001"), null, Set.of(), Set.of());
        assertThat(fraud.score(txn, ctx).riskScore()).isGreaterThanOrEqualTo(30);
    }

    @Test @DisplayName("mock fraud destination mismatch")
    void mockFraudDestinationMismatch() {
        var trip = TestFixtures.trip("trip-1", "customer-001");
        var txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setMerchantCountry("France");
        var ctx = new FraudContext(trip, List.of(), TestFixtures.activeCard("c", "customer-001"),
                TestFixtures.activeAccount("a", "customer-001"), null, Set.of(), Set.of());
        assertThat(fraud.score(txn, ctx).signals()).contains("DESTINATION_MISMATCH");
    }

    @Test @DisplayName("mock fraud ATM signal")
    void mockFraudAtmSignal() {
        var txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setTransactionType(com.travelassistant.model.Enums.TransactionType.ATM_WITHDRAWAL);
        var ctx = new FraudContext(null, List.of(), TestFixtures.activeCard("c", "customer-001"),
                TestFixtures.activeAccount("a", "customer-001"), null, Set.of(), Set.of());
        assertThat(fraud.score(txn, ctx).signals()).contains("ATM");
    }
}
