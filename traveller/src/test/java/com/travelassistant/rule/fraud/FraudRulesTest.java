package com.travelassistant.rule.fraud;

import com.travelassistant.model.Enums;
import com.travelassistant.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FraudRulesTest {
    private final OutsideTripDateRule outsideTripDate = new OutsideTripDateRule(30);
    private final OutsideDestinationRule outsideDestination = new OutsideDestinationRule(35);
    private final UnexpectedCurrencyRule unexpectedCurrency = new UnexpectedCurrencyRule(15);
    private final DuplicateTransactionRule duplicate = new DuplicateTransactionRule(40);
    private final HighValueTransactionRule highValue = new HighValueTransactionRule(30);
    private final UnusualAtmWithdrawalRule unusualAtm = new UnusualAtmWithdrawalRule(45);
    private final RapidCountryChangeRule rapidCountry = new RapidCountryChangeRule(60);
    private final MultipleDeclinesRule multipleDeclines = new MultipleDeclinesRule(30);
    private final DeclineThenApprovalRule declineThenApproval = new DeclineThenApprovalRule(40);
    private final NewMerchantCategoryRule newCategory = new NewMerchantCategoryRule(15);
    private final CardFrozenTransactionRule frozenCard = new CardFrozenTransactionRule(80);
    private final HighRiskCountryRule highRiskCountry = new HighRiskCountryRule(60);

    @Test
    void outsideTripDateTriggersWhenNoTripOrOutsideRange() {
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        assertThat(outsideTripDate.evaluate(txn, TestDataFactory.fraudContext(null, card, account, List.of())).triggered()).isTrue();

        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        assertThat(outsideTripDate.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of())).triggered()).isFalse();
    }

    @Test
    void outsideDestinationTriggersOnCountryMismatch() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("t1", "France", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        assertThat(outsideDestination.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of())).triggered()).isTrue();
    }

    @Test
    void unexpectedCurrencyTriggersForWrongCurrency() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        txn.setOriginalCurrency("EUR");
        assertThat(unexpectedCurrency.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of())).triggered()).isTrue();
    }

    @Test
    void duplicateTransactionDetectsSameMerchantAndAmountWithinTenMinutes() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var prior = TestDataFactory.txn("t0", "Japan", new BigDecimal("210"), Enums.TransactionStatus.APPROVED);
        prior.setMerchantName("Ginza Store");
        prior.setTransactionTime(Instant.parse("2026-10-02T10:00:00Z"));
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("210"), Enums.TransactionStatus.APPROVED);
        txn.setMerchantName("Ginza Store");
        txn.setTransactionTime(Instant.parse("2026-10-02T10:05:00Z"));
        assertThat(duplicate.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of(prior))).triggered()).isTrue();
    }

    @Test
    void highValueTransactionTriggersAboveThreshold() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("1500"), Enums.TransactionStatus.APPROVED);
        assertThat(highValue.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of())).triggered()).isTrue();
    }

    @Test
    void unusualAtmWithdrawalTriggersAfterTwoAtmsInTwoHours() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var prior1 = TestDataFactory.txn("t0", "Japan", new BigDecimal("100"), Enums.TransactionStatus.APPROVED);
        prior1.setTransactionType(Enums.TransactionType.ATM_WITHDRAWAL);
        prior1.setTransactionTime(Instant.parse("2026-10-02T08:30:00Z"));
        var prior2 = TestDataFactory.txn("t0b", "Japan", new BigDecimal("100"), Enums.TransactionStatus.APPROVED);
        prior2.setTransactionType(Enums.TransactionType.ATM_WITHDRAWAL);
        prior2.setTransactionTime(Instant.parse("2026-10-02T09:00:00Z"));
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("100"), Enums.TransactionStatus.APPROVED);
        txn.setTransactionType(Enums.TransactionType.ATM_WITHDRAWAL);
        txn.setTransactionTime(Instant.parse("2026-10-02T10:00:00Z"));
        assertThat(unusualAtm.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of(prior1, prior2))).triggered()).isTrue();
    }

    @Test
    void rapidCountryChangeTriggersWithinThreeHours() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var prior = TestDataFactory.txn("t0", "France", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        prior.setTransactionTime(Instant.parse("2026-10-02T08:00:00Z"));
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        txn.setTransactionTime(Instant.parse("2026-10-02T10:00:00Z"));
        assertThat(rapidCountry.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of(prior))).triggered()).isTrue();
    }

    @Test
    void multipleDeclinesTriggersAfterThreeDeclinesInOneHour() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var declines = List.of(
                declined("d1", "2026-10-02T09:50:00Z"),
                declined("d2", "2026-10-02T09:55:00Z"),
                declined("d3", "2026-10-02T09:58:00Z"));
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        txn.setTransactionTime(Instant.parse("2026-10-02T10:00:00Z"));
        assertThat(multipleDeclines.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, declines)).triggered()).isTrue();
    }

    @Test
    void declineThenApprovalTriggersOnLargeApprovalAfterDecline() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var prior = declined("d1", "2026-10-02T09:50:00Z");
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("800"), Enums.TransactionStatus.APPROVED);
        txn.setTransactionTime(Instant.parse("2026-10-02T10:00:00Z"));
        assertThat(declineThenApproval.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of(prior))).triggered()).isTrue();
    }

    @Test
    void newMerchantCategoryTriggersForUnseenCategory() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var prior = TestDataFactory.txn("t0", "Japan", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        prior.setMerchantCategory("HOTEL");
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        txn.setMerchantCategory("LUXURY");
        assertThat(newCategory.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of(prior))).triggered()).isTrue();
    }

    @Test
    void frozenCardRuleTriggersWhenCardFrozen() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.FROZEN);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("t1", "Japan", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        assertThat(frozenCard.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of())).triggered()).isTrue();
    }

    @Test
    void highRiskCountryTriggersForConfiguredCountries() {
        var trip = TestDataFactory.trip("trip", "c1", "Japan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", BigDecimal.TEN, Enums.AccountStatus.ACTIVE);
        var txn = TestDataFactory.txn("t1", "Iran", new BigDecimal("50"), Enums.TransactionStatus.APPROVED);
        assertThat(highRiskCountry.evaluate(txn, TestDataFactory.fraudContext(trip, card, account, List.of())).triggered()).isTrue();
    }

    private static com.travelassistant.model.TravelTransaction declined(String id, String time) {
        var txn = TestDataFactory.txn(id, "Japan", new BigDecimal("50"), Enums.TransactionStatus.DECLINED);
        txn.setTransactionTime(Instant.parse(time));
        return txn;
    }
}
