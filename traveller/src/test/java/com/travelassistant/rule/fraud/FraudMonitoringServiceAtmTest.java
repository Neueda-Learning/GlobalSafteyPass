package com.travelassistant.rule.fraud;

import com.travelassistant.TestFixtures;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.integration.FraudDetectionClient;
import com.travelassistant.model.*;
import com.travelassistant.repository.FraudAlertRepository;
import com.travelassistant.service.AuditService;
import com.travelassistant.service.FraudMonitoringService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudMonitoringServiceAtmTest {
    @Mock FraudDetectionClient external;
    @Mock FraudAlertRepository alerts;
    @Mock AuditService audit;
    FraudMonitoringService service;

    @BeforeEach
    void setUp() {
        service = new FraudMonitoringService(
                List.of(new UnusualAtmWithdrawalRule(45)),
                external, alerts, audit, 50, 0.50, 0.30, 0.20);
    }

    @Test
    void firstAtmWithdrawalAddsProfileAnomalyPoints() {
        when(external.score(any(), any())).thenReturn(new ExternalFraudResult(0, List.of(), "ref", "mock"));
        Trip trip = TestFixtures.trip("trip-1", "customer-001", "Japan", "Tokyo", Enums.TripStatus.ACTIVE);
        Card card = TestFixtures.card("card-001", "customer-001", new BigDecimal("500"));
        TravelTransaction atm = TestFixtures.atmWithdrawal("txn-atm", Instant.now());
        FraudContext context = new FraudContext(trip, List.of(), card,
                TestFixtures.account("acct-001", "customer-001"), new BigDecimal("150"),
                java.util.Set.of("Japan"), java.util.Set.of("FOOD", "ATM"));

        FraudAssessment assessment = service.evaluate(atm, context);

        assertThat(assessment.bankProfileScore()).isEqualTo(15);
    }

    @Test
    void repeatedAtmWithdrawalsTriggerUnusualAtmRule() {
        when(external.score(any(), any())).thenReturn(new ExternalFraudResult(10, List.of("ATM"), "ref", "mock"));
        Instant now = Instant.parse("2026-08-12T10:00:00Z");
        Trip trip = TestFixtures.trip("trip-1", "customer-001", "Japan", "Tokyo", Enums.TripStatus.ACTIVE);
        Card card = TestFixtures.card("card-001", "customer-001", new BigDecimal("500"));
        List<TravelTransaction> recent = List.of(
                TestFixtures.atmWithdrawal("txn-1", now.minusSeconds(3600)),
                TestFixtures.atmWithdrawal("txn-2", now.minusSeconds(1800)));
        TravelTransaction current = TestFixtures.atmWithdrawal("txn-3", now);
        FraudContext context = new FraudContext(trip, recent, card,
                TestFixtures.account("acct-001", "customer-001"), new BigDecimal("150"),
                java.util.Set.of("Japan"), java.util.Set.of("ATM"));

        FraudAssessment assessment = service.evaluate(current, context);

        assertThat(assessment.internalScore()).isEqualTo(45);
        assertThat(assessment.reasons()).contains("UNUSUAL_ATM_WITHDRAWAL");
    }
}
