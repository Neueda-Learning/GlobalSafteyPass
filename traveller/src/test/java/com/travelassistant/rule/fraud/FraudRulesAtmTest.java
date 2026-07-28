package com.travelassistant.rule.fraud;

import com.travelassistant.TestFixtures;
import com.travelassistant.dto.ApiDtos.FraudRuleResult;
import com.travelassistant.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class FraudRulesAtmTest {
    private final UnusualAtmWithdrawalRule rule = new UnusualAtmWithdrawalRule(45);
    private final Instant now = Instant.parse("2026-08-12T10:00:00Z");
    private Trip trip;
    private Card card;

    @BeforeEach
    void setUp() {
        trip = TestFixtures.trip("trip-1", "customer-001", "Japan", "Tokyo", Enums.TripStatus.ACTIVE);
        card = TestFixtures.card("card-001", "customer-001", new BigDecimal("500"));
    }

    @Test
    void doesNotTriggerForNonAtmTransaction() {
        TravelTransaction purchase = TravelTransaction.builder()
                .transactionId("txn-1").transactionType(Enums.TransactionType.PURCHASE)
                .transactionTime(now).status(Enums.TransactionStatus.APPROVED).build();
        FraudRuleResult result = rule.evaluate(purchase, TestFixtures.fraudContext(trip, card, List.of()));

        assertThat(result.triggered()).isFalse();
        assertThat(result.ruleCode()).isEqualTo("UNUSUAL_ATM_WITHDRAWAL");
    }

    @Test
    void doesNotTriggerForFirstAtmWithdrawal() {
        TravelTransaction atm = TestFixtures.atmWithdrawal("txn-1", now);
        FraudRuleResult result = rule.evaluate(atm, TestFixtures.fraudContext(trip, card, List.of()));

        assertThat(result.triggered()).isFalse();
        assertThat(result.riskPoints()).isZero();
    }

    @Test
    void doesNotTriggerForSecondAtmWithinTwoHours() {
        List<TravelTransaction> recent = List.of(
                TestFixtures.atmWithdrawal("txn-1", now.minusSeconds(3600)));
        TravelTransaction current = TestFixtures.atmWithdrawal("txn-2", now);
        FraudRuleResult result = rule.evaluate(current, TestFixtures.fraudContext(trip, card, recent));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void triggersWhenThreeAtmWithdrawalsWithinTwoHours() {
        List<TravelTransaction> recent = List.of(
                TestFixtures.atmWithdrawal("txn-1", now.minusSeconds(3600)),
                TestFixtures.atmWithdrawal("txn-2", now.minusSeconds(1800)));
        TravelTransaction current = TestFixtures.atmWithdrawal("txn-3", now);
        FraudRuleResult result = rule.evaluate(current, TestFixtures.fraudContext(trip, card, recent));

        assertThat(result.triggered()).isTrue();
        assertThat(result.riskPoints()).isEqualTo(45);
        assertThat(result.riskLevel()).isEqualTo(Enums.RiskLevel.MEDIUM);
        assertThat(result.customerReason()).contains("ATM");
    }

    @Test
    void doesNotCountAtmWithdrawalsOlderThanTwoHours() {
        List<TravelTransaction> recent = List.of(
                TestFixtures.atmWithdrawal("txn-1", now.minusSeconds(3 * 3600)),
                TestFixtures.atmWithdrawal("txn-2", now.minusSeconds(2 * 3600 + 60)));
        TravelTransaction current = TestFixtures.atmWithdrawal("txn-3", now);
        FraudRuleResult result = rule.evaluate(current, TestFixtures.fraudContext(trip, card, recent));

        assertThat(result.triggered()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {45, 60, 75})
    void respectsConfiguredRiskPoints(int points) {
        UnusualAtmWithdrawalRule customRule = new UnusualAtmWithdrawalRule(points);
        List<TravelTransaction> recent = List.of(
                TestFixtures.atmWithdrawal("txn-1", now.minusSeconds(1800)),
                TestFixtures.atmWithdrawal("txn-2", now.minusSeconds(900)));
        FraudRuleResult result = customRule.evaluate(
                TestFixtures.atmWithdrawal("txn-3", now),
                TestFixtures.fraudContext(trip, card, recent));

        assertThat(result.riskPoints()).isEqualTo(points);
    }
}
