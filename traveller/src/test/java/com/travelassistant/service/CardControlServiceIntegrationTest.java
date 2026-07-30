package com.travelassistant.service;

import com.travelassistant.model.Enums;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class CardControlServiceIntegrationTest {
    @Autowired CardControlService cardControl;

    @Test
    void listReturnsOnlyCustomerCards() {
        var cards = cardControl.list("customer-001");
        assertThat(cards).isNotEmpty();
        assertThat(cards).allMatch(c -> !c.id().equals("card-201"));
    }

    @Test
    void listDoesNotReturnAnotherCustomersCards() {
        var cards = cardControl.list("customer-002");
        assertThat(cards).noneMatch(c -> c.id().equals("card-001"));
        assertThat(cards).anyMatch(c -> c.id().equals("card-201"));
    }

    @Test
    void toggleOverseasAndOnlinePayments() {
        var disabled = cardControl.overseas("customer-001", "card-002", false);
        assertThat(disabled.overseasPaymentsEnabled()).isFalse();
        var enabled = cardControl.overseas("customer-001", "card-002", true);
        assertThat(enabled.overseasPaymentsEnabled()).isTrue();
        var online = cardControl.online("customer-001", "card-002", false);
        assertThat(online.onlinePaymentsEnabled()).isFalse();
    }

    @Test
    void freezeUnfreezeAndUpdateLimits() {
        cardControl.freeze("customer-001", "card-backup");
        assertThat(cardControl.list("customer-001").stream().filter(c -> c.id().equals("card-backup")).findFirst().get().status())
                .isEqualTo(Enums.CardStatus.FROZEN);
        cardControl.unfreeze("customer-001", "card-backup");
        cardControl.paymentLimit("customer-001", "card-backup", new BigDecimal("2000"));
        cardControl.withdrawalLimit("customer-001", "card-backup", new BigDecimal("800"));
        var card = cardControl.list("customer-001").stream().filter(c -> c.id().equals("card-backup")).findFirst().get();
        assertThat(card.dailyPaymentLimit()).isEqualByComparingTo(new BigDecimal("2000"));
    }
}
