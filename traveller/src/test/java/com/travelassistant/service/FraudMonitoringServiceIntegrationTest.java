package com.travelassistant.service;

import com.travelassistant.model.Enums;
import com.travelassistant.repository.FraudAlertRepository;
import com.travelassistant.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class FraudMonitoringServiceIntegrationTest {
    @Autowired FraudMonitoringService fraudMonitoring;
    @Autowired FraudAlertRepository alerts;

    @Test
    void highRiskTransactionCreatesAlert() {
        long before = alerts.count();
        var trip = TestDataFactory.trip("trip-paris", "customer-001", "France",
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("account-001", new BigDecimal("10000"), Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("txn-fraud-test", "Iran", new BigDecimal("2000"), Enums.TransactionStatus.APPROVED);
        txn.setCustomerId("customer-001");
        txn.setTripId("trip-paris");
        var assessment = fraudMonitoring.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, java.util.List.of()));
        assertThat(assessment.finalScore()).isGreaterThanOrEqualTo(50);
        assertThat(alerts.count()).isGreaterThan(before);
    }

    @Test
    void lowRiskTransactionDoesNotCreateAlert() {
        long before = alerts.count();
        var trip = TestDataFactory.trip("trip-paris", "customer-001", "France",
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("account-001", new BigDecimal("10000"), Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("txn-low-risk", "France", new BigDecimal("25"), Enums.TransactionStatus.APPROVED);
        txn.setCustomerId("customer-001");
        txn.setTripId("trip-paris");
        txn.setTransactionTime(java.time.Instant.parse("2026-11-06T12:00:00Z"));
        var assessment = fraudMonitoring.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, java.util.List.of()));
        assertThat(assessment.finalScore()).isLessThan(50);
        assertThat(alerts.count()).isEqualTo(before);
    }
}
