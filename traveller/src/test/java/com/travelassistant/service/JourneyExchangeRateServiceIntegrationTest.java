package com.travelassistant.service;

import com.travelassistant.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class JourneyExchangeRateServiceIntegrationTest {
    @Autowired JourneyExchangeRateService journeyExchangeRateService;

    @Test
    void getForMappedDestinationReturnsRateAndSupportFlag() {
        var response = journeyExchangeRateService.get("customer-001", "trip-paris");
        assertThat(response.tripId()).isEqualTo("trip-paris");
        assertThat(response.destinationCurrency()).isEqualTo("EUR");
        assertThat(response.rate()).isNotNull();
        assertThat(response.destinationCurrencyVerified()).isTrue();
        assertThat(response.cardSupportsCurrency()).isTrue();
    }

    @Test
    void getForUnmappedDestinationUsesBudgetCurrencyFallback() {
        var response = journeyExchangeRateService.get("customer-001", "trip-reykjavik-demo");
        assertThat(response.destinationCurrency()).isEqualTo("USD");
        assertThat(response.destinationCurrencyVerified()).isTrue();
        assertThat(response.cardSupportsCurrency()).isTrue();
        assertThat(response.recommendation()).containsIgnoringCase("fallback");
    }

    @Test
    void getRejectsAnotherCustomersTrip() {
        assertThatThrownBy(() -> journeyExchangeRateService.get("customer-002", "trip-paris"))
                .isInstanceOf(ForbiddenException.class);
    }
}
