package com.travelassistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class TravelCardRecommendationServiceIntegrationTest {
    @Autowired TravelCardRecommendationService recommendationService;

    @Test
    void eligibleCardsExcludesCurrentCard() {
        var options = recommendationService.eligibleCards("customer-001", "card-001",
                new BigDecimal("50"), "EUR");
        assertThat(options).noneMatch(c -> c.cardId().equals("card-001"));
    }

    @Test
    void eligibleCardsReturnsActiveCardsWithSufficientFunds() {
        var options = recommendationService.eligibleCards("customer-001", "card-002",
                new BigDecimal("50"), "EUR");
        assertThat(options).isNotEmpty();
        assertThat(options).allMatch(c -> c.maskedCardNumber().contains("****"));
    }

    @Test
    void eligibleCardsEmptyWhenAmountExceedsAllBalances() {
        var options = recommendationService.eligibleCards("customer-001", "card-001",
                new BigDecimal("999999"), "USD");
        assertThat(options).isEmpty();
    }

    @Test
    void eligibleCardsFiltersOutFrozenCards() {
        var options = recommendationService.eligibleCards("customer-001", "card-001",
                new BigDecimal("50"), "USD");
        assertThat(options).noneMatch(c -> c.cardId().equals("card-frozen"));
    }
}
