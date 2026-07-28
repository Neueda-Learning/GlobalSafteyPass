package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardCapabilityService")
class CardCapabilityServiceTest {
    private CardCapabilityService service;
    private Card card;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new CardCapabilityService();
        card = TestFixtures.activeCard("card-001", "customer-001");
        account = TestFixtures.activeAccount("acct-001", "customer-001");
    }

    @Test @DisplayName("parses supported currencies")
    void parsesSupportedCurrencies() {
        assertThat(service.supportedCurrencies(card)).containsExactly("USD", "JPY", "EUR");
    }

    @Test @DisplayName("blank supported currencies")
    void blankSupportedCurrencies() {
        card.setSupportedCurrencies("");
        assertThat(service.supportedCurrencies(card)).isEmpty();
    }

    @Test @DisplayName("main currency supported")
    void mainCurrencySupported() {
        assertThat(service.supportsCurrency(card, "USD")).isTrue();
    }

    @Test @DisplayName("listed currency supported")
    void listedCurrencySupported() {
        assertThat(service.supportsCurrency(card, "JPY")).isTrue();
    }

    @Test @DisplayName("unsupported currency rejected")
    void unsupportedCurrency() {
        assertThat(service.supportsCurrency(card, "THB")).isFalse();
    }

    @Test @DisplayName("null currency treated as supported")
    void nullCurrencySupported() {
        assertThat(service.supportsCurrency(card, null)).isTrue();
    }

    @Test @DisplayName("active card can pay")
    void canCompleteTravelPayment() {
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("500"), "JPY")).isTrue();
    }

    @Test @DisplayName("frozen card cannot pay")
    void frozenCardCannotPay() {
        card.setStatus(Enums.CardStatus.FROZEN);
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isFalse();
    }

    @Test @DisplayName("overseas disabled blocks payment")
    void overseasDisabledBlocksPayment() {
        card.setOverseasPaymentsEnabled(false);
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isFalse();
    }

    @Test @DisplayName("insufficient balance blocks payment")
    void insufficientBalanceBlocksPayment() {
        account.setAvailableBalance(new BigDecimal("50"));
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isFalse();
    }

    @Test @DisplayName("inactive account blocks payment")
    void inactiveAccountBlocksPayment() {
        account.setStatus(Enums.AccountStatus.FROZEN);
        assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isFalse();
    }
}
