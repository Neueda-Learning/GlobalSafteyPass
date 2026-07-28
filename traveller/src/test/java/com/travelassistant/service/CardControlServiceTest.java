package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.integration.CardSystemClient;
import com.travelassistant.model.*;
import com.travelassistant.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CardControlService")
class CardControlServiceTest {
    private CardRepository cards;
    private CardSystemClient client;
    private AuditService audit;
    private CardControlService service;

    @BeforeEach
    void setUp() {
        cards = mock(CardRepository.class);
        client = mock(CardSystemClient.class);
        audit = mock(AuditService.class);
        service = new CardControlService(cards, client, audit);
    }

    @Test @DisplayName("lists customer cards")
    void listsCustomerCards() {
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        when(cards.findByCustomerId("customer-001")).thenReturn(List.of(card));
        assertThat(service.list("customer-001")).hasSize(1);
    }

    @Test @DisplayName("freeze card")
    void freezeCard() {
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        service.freeze("customer-001", "card-001");
        verify(client).freeze(card);
        verify(audit).log("customer-001", "CARD_FROZEN", "CARD", "card-001", "Card frozen");
    }

    @Test @DisplayName("enable overseas payments")
    void enableOverseasPayments() {
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        service.overseas("customer-001", "card-001", true);
        verify(client).setOverseasPayments(card, true);
    }

    @Test @DisplayName("update payment limit")
    void updatePaymentLimit() {
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        service.paymentLimit("customer-001", "card-001", new BigDecimal("8000"));
        verify(client).setPaymentLimit(card, new BigDecimal("8000"));
    }
}
