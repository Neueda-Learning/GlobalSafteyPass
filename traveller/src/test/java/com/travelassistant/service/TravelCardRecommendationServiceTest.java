package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("TravelCardRecommendationService")
class TravelCardRecommendationServiceTest {
    private CardRepository cards;
    private AccountRepository accounts;
    private TravelCardRecommendationService service;

    @BeforeEach
    void setUp() {
        cards = mock(CardRepository.class);
        accounts = mock(AccountRepository.class);
        service = new TravelCardRecommendationService(cards, accounts, new CardCapabilityService());
    }

    @Test @DisplayName("returns eligible backup cards")
    void returnsEligibleBackupCards() {
        Card primary = TestFixtures.activeCard("card-001", "customer-001");
        Card backup = TestFixtures.activeCard("card-002", "customer-001");
        Account acct = TestFixtures.activeAccount("acct-001", "customer-001");
        when(cards.findByCustomerId("customer-001")).thenReturn(List.of(primary, backup));
        when(accounts.findById("acct-001")).thenReturn(Optional.of(acct));
        var options = service.eligibleCards("customer-001", "card-001", new BigDecimal("100"), "JPY");
        assertThat(options).hasSize(1);
        assertThat(options.get(0).cardId()).isEqualTo("card-002");
    }

    @Test @DisplayName("excludes current card")
    void excludesCurrentCard() {
        Card only = TestFixtures.activeCard("card-001", "customer-001");
        when(cards.findByCustomerId("customer-001")).thenReturn(List.of(only));
        assertThat(service.eligibleCards("customer-001", "card-001", new BigDecimal("100"), "JPY")).isEmpty();
    }
}
