package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.integration.FraudDetectionClient;
import com.travelassistant.model.*;
import com.travelassistant.repository.FraudAlertRepository;
import com.travelassistant.rule.fraud.FraudContext;
import com.travelassistant.rule.fraud.FraudRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("FraudMonitoringService")
class FraudMonitoringServiceTest {
    private FraudAlertRepository alerts;
    private AuditService audit;
    private FraudMonitoringService service;
    private FraudContext ctx;
    private TravelTransaction txn;

    @BeforeEach
    void setUp() {
        alerts = mock(FraudAlertRepository.class);
        audit = mock(AuditService.class);
        List<FraudRule> rules = List.of(
                triggeredRule("OUTSIDE_DESTINATION", 35),
                triggeredRule("HIGH_VALUE_TRANSACTION", 30),
                (t, c) -> new FraudRuleResult("CARD_FROZEN_TRANSACTION", false, 0, Enums.RiskLevel.LOW, "", ""));
        FraudDetectionClient external = (t, c) -> new ExternalFraudResult(10, List.of(), "ext-1", "Mock");
        service = new FraudMonitoringService(rules, external, alerts, audit, 50, 0.5, 0.3, 0.2);
        Trip trip = TestFixtures.trip("trip-1", "customer-001");
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        Account account = TestFixtures.activeAccount("acct-001", "customer-001");
        txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setMerchantCountry("France");
        txn.setBillingAmount(new BigDecimal("1500"));
        ctx = new FraudContext(trip, List.of(), card, account, new BigDecimal("200"), Set.of("Japan"), Set.of("SHOPPING"));
    }

    @Test @DisplayName("high risk creates alert")
    void highRiskCreatesAlert() {
        FraudAssessment result = service.evaluate(txn, ctx);
        assertThat(result.finalScore()).isGreaterThanOrEqualTo(50);
        verify(alerts).save(any(FraudAlert.class));
        verify(audit).log(eq("customer-001"), eq("FRAUD_ALERT_CREATED"), eq("ALERT"), anyString(), anyString());
    }

    @Test @DisplayName("low risk allows transaction")
    void lowRiskAllowsTransaction() {
        txn.setMerchantCountry("Japan");
        txn.setBillingAmount(new BigDecimal("50"));
        FraudDetectionClient external = (t, c) -> new ExternalFraudResult(5, List.of(), "ext-1", "Mock");
        var lowRiskService = new FraudMonitoringService(
                List.of((t, c) -> new FraudRuleResult("SAFE", false, 0, Enums.RiskLevel.LOW, "", "")),
                external, alerts, audit, 50, 0.5, 0.3, 0.2);
        FraudAssessment result = lowRiskService.evaluate(txn, ctx);
        assertThat(result.decision()).isIn(Enums.FraudDecision.ALLOW, Enums.FraudDecision.MONITOR);
        verify(alerts, never()).save(any());
    }

    @Test @DisplayName("external failure still scores")
    void externalFailureStillScores() {
        FraudDetectionClient failing = (t, c) -> { throw new RuntimeException("down"); };
        var svc = new FraudMonitoringService(List.of(triggeredRule("OUTSIDE_DESTINATION", 35)), failing, alerts, audit, 50, 0.5, 0.3, 0.2);
        assertThat(svc.evaluate(txn, ctx).finalScore()).isGreaterThan(0);
    }

    private static FraudRule triggeredRule(String code, int points) {
        return (t, c) -> new FraudRuleResult(code, true, points, Enums.RiskLevel.HIGH, "customer", "internal");
    }
}
