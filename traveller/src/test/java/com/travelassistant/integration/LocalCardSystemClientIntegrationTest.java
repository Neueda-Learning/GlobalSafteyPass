package com.travelassistant.integration;

import com.travelassistant.model.Enums;
import com.travelassistant.repository.CardRepository;
import com.travelassistant.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class LocalCardSystemClientIntegrationTest {
    @Autowired LocalCardSystemClient client;
    @Autowired CardRepository cards;

    @Test
    void cardOperationsPersistToDatabase() {
        var card = cards.findById("card-002").orElseThrow();
        client.setOverseasPayments(card, false);
        assertThat(cards.findById("card-002").orElseThrow().isOverseasPaymentsEnabled()).isFalse();
        client.setPaymentLimit(card, new BigDecimal("2500"));
        assertThat(cards.findById("card-002").orElseThrow().getDailyPaymentLimit())
                .isEqualByComparingTo(new BigDecimal("2500"));
        client.freeze(card);
        assertThat(cards.findById("card-002").orElseThrow().getStatus()).isEqualTo(Enums.CardStatus.FROZEN);
        client.unfreeze(cards.findById("card-002").orElseThrow());
        assertThat(cards.findById("card-002").orElseThrow().getStatus()).isEqualTo(Enums.CardStatus.ACTIVE);
    }
}
