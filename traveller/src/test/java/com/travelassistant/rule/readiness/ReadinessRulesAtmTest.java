package com.travelassistant.rule.readiness;

import com.travelassistant.TestFixtures;
import com.travelassistant.dto.ApiDtos.ReadinessRuleResult;
import com.travelassistant.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ReadinessRulesAtmTest {
    private final WithdrawalLimitRule rule = new WithdrawalLimitRule();
    private Trip trip;
    private Account account;

    @BeforeEach
    void setUp() {
        trip = TestFixtures.trip("trip-1", "customer-001", "Japan", "Tokyo", Enums.TripStatus.PLANNED);
        account = TestFixtures.account("acct-001", "customer-001");
    }

    @Test
    void passesWhenWithdrawalLimitIsAtLeast100() {
        Card card = TestFixtures.card("card-001", "customer-001", new BigDecimal("100"));
        ReadinessRuleResult result = rule.evaluate(trip, card, account);

        assertThat(result.passed()).isTrue();
        assertThat(result.ruleCode()).isEqualTo("WITHDRAWAL_LIMIT");
        assertThat(result.message()).contains("ATM");
    }

    @Test
    void passesWhenWithdrawalLimitExceeds100() {
        Card card = TestFixtures.card("card-001", "customer-001", new BigDecimal("1000"));
        ReadinessRuleResult result = rule.evaluate(trip, card, account);

        assertThat(result.passed()).isTrue();
    }

    @Test
    void failsWhenWithdrawalLimitBelow100() {
        Card card = TestFixtures.card("card-001", "customer-001", new BigDecimal("50"));
        ReadinessRuleResult result = rule.evaluate(trip, card, account);

        assertThat(result.passed()).isFalse();
        assertThat(result.ruleCode()).isEqualTo("WITHDRAWAL_LIMIT_LOW");
        assertThat(result.scoreImpact()).isEqualTo(-5);
        assertThat(result.severity()).isEqualTo(Enums.Severity.WARNING);
        assertThat(result.recommendedAction()).contains("withdrawal limit");
    }

    @ParameterizedTest
    @CsvSource({"0", "50", "99.99"})
    void failsForLimitsBelowThreshold(String limit) {
        Card card = TestFixtures.card("card-001", "customer-001", new BigDecimal(limit));
        assertThat(rule.evaluate(trip, card, account).passed()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"100", "500", "5000"})
    void passesForLimitsAtOrAboveThreshold(String limit) {
        Card card = TestFixtures.card("card-001", "customer-001", new BigDecimal(limit));
        assertThat(rule.evaluate(trip, card, account).passed()).isTrue();
    }
}
