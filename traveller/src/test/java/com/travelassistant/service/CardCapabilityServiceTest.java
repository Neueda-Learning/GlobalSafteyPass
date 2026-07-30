package com.travelassistant.service;

import com.travelassistant.model.Enums;
import com.travelassistant.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CardCapabilityServiceTest {
    private final CardCapabilityService service = new CardCapabilityService();

    @Test
    void supportsMainCurrencyReturnsTrue() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        assertThat(service.supportsCurrency(card, "USD")).isTrue();
    }

    @Test
    void supportsListedCurrencyReturnsTrue() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        assertThat(service.supportsCurrency(card, "EUR")).isTrue();
    }

    @Test
    void supportsUnlistedCurrencyReturnsFalse() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        assertThat(service.supportsCurrency(card, "JPY")).isFalse();
    }

    @Test
    void canCompleteTravelPaymentReturnsTrueWhenAllConditionsMet() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("account-001", new BigDecimal("1000"), Enums.AccountStatus.ACTIVE);
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isTrue();
    }

    @Test
    void canCompleteTravelPaymentReturnsFalseWhenCardFrozen() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.FROZEN);
        var account = TestDataFactory.account("account-001", new BigDecimal("1000"), Enums.AccountStatus.ACTIVE);
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isFalse();
    }

    @Test
    void canCompleteTravelPaymentReturnsFalseWhenOverseasDisabled() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        card.setOverseasPaymentsEnabled(false);
        var account = TestDataFactory.account("account-001", new BigDecimal("1000"), Enums.AccountStatus.ACTIVE);
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isFalse();
    }

    @Test
    void canCompleteTravelPaymentReturnsFalseWhenInsufficientFunds() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("account-001", new BigDecimal("10"), Enums.AccountStatus.ACTIVE);
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isFalse();
    }

    @Test
    void emptySupportedCurrenciesReturnsEmptyList() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        card.setSupportedCurrencies(null);
        assertThat(service.supportedCurrencies(card)).isEmpty();
    }

    @Test
    void blankSupportedCurrenciesReturnsEmptyList() {
        var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
        card.setSupportedCurrencies("   ");
        assertThat(service.supportedCurrencies(card)).isEmpty();
    }
}
