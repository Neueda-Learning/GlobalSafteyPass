package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.model.Trip;
import com.travelassistant.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "integration.fx.enabled=false")
@ActiveProfiles("test")
@Transactional
class CashExchangePlanningServiceIntegrationTest {
    @Autowired CashExchangePlanningService cashPlanning;
    @Autowired TripRepository trips;
    @Autowired CardRepository cards;
    @Autowired AuditLogRepository auditLogs;

    @BeforeEach
    void seed() {
        cards.save(TestFixtures.card("card-001", "customer-001", new BigDecimal("500")));
        trips.save(TestFixtures.trip("trip-japan", "customer-001", "Japan", "Tokyo", com.travelassistant.model.Enums.TripStatus.PLANNED));
    }

    @Test
    void savesCashExchangePlanWithAtmAvailabilityForJapan() {
        CashExchangePlanRequest request = new CashExchangePlanRequest(
                new BigDecimal("200"), "ATM withdrawal on arrival", "Narita Airport");

        CashExchangePlanResponse response = cashPlanning.save("customer-001", "trip-japan", request);

        assertThat(response.tripId()).isEqualTo("trip-japan");
        assertThat(response.destinationCurrency()).isEqualTo("JPY");
        assertThat(response.atmAvailability()).contains("Japan Post ATMs");
        assertThat(response.feeAdvice()).contains("bank-owned ATM");
        Trip updated = trips.findById("trip-japan").orElseThrow();
        assertThat(updated.isCashExchangePlanned()).isTrue();
        assertThat(updated.getCashExchangeMethod()).isEqualTo("ATM withdrawal on arrival");
    }

    @Test
    void providesGenericAtmAdviceForUnknownDestination() {
        trips.save(TestFixtures.trip("trip-unknown", "customer-001", "Brazil", "Rio",
                com.travelassistant.model.Enums.TripStatus.PLANNED));
        CashExchangePlanRequest request = new CashExchangePlanRequest(
                new BigDecimal("100"), "Cash exchange", "Airport");

        CashExchangePlanResponse response = cashPlanning.save("customer-001", "trip-unknown", request);

        assertThat(response.atmAvailability()).contains("bank-owned ATM");
        assertThat(response.destinationCurrency()).isNull();
    }

    @Test
    void recordsAuditLogForCashExchangePlan() {
        CashExchangePlanRequest request = new CashExchangePlanRequest(
                new BigDecimal("150"), "ATM", "City centre");
        cashPlanning.save("customer-001", "trip-japan", request);

        assertThat(auditLogs.findAll()).anyMatch(log ->
                "CASH_EXCHANGE_PLAN_RECORDED".equals(log.getAction()));
    }
}
