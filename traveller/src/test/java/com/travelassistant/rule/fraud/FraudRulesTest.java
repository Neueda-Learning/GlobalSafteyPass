package com.travelassistant.rule.fraud;

import com.travelassistant.TestFixtures;
import com.travelassistant.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import static com.travelassistant.model.Enums.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fraud Rules")
class FraudRulesTest {
    private Trip trip;
    private Card card;
    private Account account;
    private FraudContext ctx;

    @BeforeEach
    void setUp() {
        trip = TestFixtures.trip("trip-1", "customer-001");
        card = TestFixtures.activeCard("card-001", "customer-001");
        account = TestFixtures.activeAccount("acct-001", "customer-001");
        ctx = new FraudContext(trip, List.of(), card, account, new BigDecimal("200"), Set.of("Japan"), Set.of("SHOPPING"));
    }

    private TravelTransaction txn(Instant time, String country, String currency, BigDecimal amount) {
        return TravelTransaction.builder()
                .transactionId("txn-" + time.toEpochMilli()).customerId("customer-001").tripId("trip-1")
                .cardId("card-001").merchantName("Store").merchantCountry(country).merchantCity("City")
                .merchantCategory("SHOPPING").originalAmount(amount).originalCurrency(currency)
                .billingAmount(amount).billingCurrency("USD").transactionTime(time)
                .transactionType(TransactionType.PURCHASE).status(TransactionStatus.APPROVED).build();
    }

    @Test @DisplayName("outside destination triggers")
    void outsideDestinationTriggers() {
        var rule = new OutsideDestinationRule(35);
        var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "France", "EUR", new BigDecimal("100"));
        assertThat(rule.evaluate(t, ctx).triggered()).isTrue();
    }

    @Test @DisplayName("matching destination passes")
    void matchingDestinationPasses() {
        var rule = new OutsideDestinationRule(35);
        var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "JPY", new BigDecimal("100"));
        assertThat(rule.evaluate(t, ctx).triggered()).isFalse();
    }

    @Test @DisplayName("outside trip date triggers")
    void outsideTripDateTriggers() {
        var rule = new OutsideTripDateRule(30);
        var t = txn(Instant.parse("2026-07-01T10:00:00Z"), "Japan", "JPY", new BigDecimal("50"));
        assertThat(rule.evaluate(t, ctx).triggered()).isTrue();
    }

    @Test @DisplayName("within trip date passes")
    void withinTripDatePasses() {
        var rule = new OutsideTripDateRule(30);
        var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "JPY", new BigDecimal("50"));
        assertThat(rule.evaluate(t, ctx).triggered()).isFalse();
    }

    @Test @DisplayName("unexpected currency for Japan")
    void unexpectedCurrencyForJapan() {
        var rule = new UnexpectedCurrencyRule(15);
        var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "USD", new BigDecimal("50"));
        assertThat(rule.evaluate(t, ctx).triggered()).isTrue();
    }

    @Test @DisplayName("expected JPY currency passes")
    void expectedJpyCurrencyPasses() {
        var rule = new UnexpectedCurrencyRule(15);
        var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "JPY", new BigDecimal("50"));
        assertThat(rule.evaluate(t, ctx).triggered()).isFalse();
    }

    @Test @DisplayName("duplicate transaction detected")
    void duplicateTransactionDetected() {
        var prior = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "JPY", new BigDecimal("120"));
        prior.setTransactionId("txn-prior");
        var current = txn(Instant.parse("2026-08-05T10:05:00Z"), "Japan", "JPY", new BigDecimal("120"));
        current.setMerchantName("Tokyo Store");
        prior.setMerchantName("Tokyo Store");
        var rule = new DuplicateTransactionRule(40);
        var c = new FraudContext(trip, List.of(prior), card, account, new BigDecimal("200"), Set.of(), Set.of());
        assertThat(rule.evaluate(current, c).triggered()).isTrue();
    }

    @Test @DisplayName("high value transaction triggers")
    void highValueTransactionTriggers() {
        var rule = new HighValueTransactionRule(30);
        var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "JPY", new BigDecimal("1500"));
        assertThat(rule.evaluate(t, ctx).triggered()).isTrue();
    }

    @Test @DisplayName("frozen card transaction triggers")
    void frozenCardTransactionTriggers() {
        card.setStatus(CardStatus.FROZEN);
        var rule = new CardFrozenTransactionRule(80);
        var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "JPY", new BigDecimal("50"));
        assertThat(rule.evaluate(t, new FraudContext(trip, List.of(), card, account, null, Set.of(), Set.of())).triggered()).isTrue();
    }

    @Test @DisplayName("high risk country triggers")
    void highRiskCountryTriggers() {
        var rule = new HighRiskCountryRule(60);
        var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "Iran", "IRR", new BigDecimal("50"));
        assertThat(rule.evaluate(t, ctx).triggered()).isTrue();
    }

    @Test @DisplayName("rapid country change triggers")
    void rapidCountryChangeTriggers() {
        var prior = txn(Instant.parse("2026-08-05T09:00:00Z"), "Japan", "JPY", new BigDecimal("50"));
        prior.setTransactionId("txn-1");
        var current = txn(Instant.parse("2026-08-05T10:00:00Z"), "France", "EUR", new BigDecimal("50"));
        var rule = new RapidCountryChangeRule(60);
        assertThat(rule.evaluate(current, new FraudContext(trip, List.of(prior), card, account, null, Set.of(), Set.of())).triggered()).isTrue();
    }

    @Test @DisplayName("multiple declines triggers")
    void multipleDeclinesTriggers() {
        var rule = new MultipleDeclinesRule(30);
        var d1 = txn(Instant.parse("2026-08-05T09:30:00Z"), "Japan", "JPY", new BigDecimal("50"));
        d1.setStatus(TransactionStatus.DECLINED); d1.setTransactionId("d1");
        var d2 = txn(Instant.parse("2026-08-05T09:40:00Z"), "Japan", "JPY", new BigDecimal("50"));
        d2.setStatus(TransactionStatus.DECLINED); d2.setTransactionId("d2");
        var d3 = txn(Instant.parse("2026-08-05T09:50:00Z"), "Japan", "JPY", new BigDecimal("50"));
        d3.setStatus(TransactionStatus.DECLINED); d3.setTransactionId("d3");
        var current = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "JPY", new BigDecimal("50"));
        var c = new FraudContext(trip, List.of(d1, d2, d3), card, account, null, Set.of(), Set.of());
        assertThat(rule.evaluate(current, c).triggered()).isTrue();
    }

    @Test @DisplayName("new merchant category triggers")
    void newMerchantCategoryTriggers() {
        var prior = txn(Instant.parse("2026-08-05T09:00:00Z"), "Japan", "JPY", new BigDecimal("50"));
        prior.setMerchantCategory("FOOD"); prior.setTransactionId("txn-food");
        var current = txn(Instant.parse("2026-08-05T10:00:00Z"), "Japan", "JPY", new BigDecimal("50"));
        current.setMerchantCategory("GAMBLING");
        var rule = new NewMerchantCategoryRule(15);
        assertThat(rule.evaluate(current, new FraudContext(trip, List.of(prior), card, account, null, Set.of(), Set.of())).triggered()).isTrue();
    }
}
