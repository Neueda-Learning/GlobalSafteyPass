package com.travelassistant.service;

import com.travelassistant.exception.ResourceNotFoundException;
import com.travelassistant.model.Enums;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class ReadinessServiceIntegrationTest {
    @Autowired ReadinessService readinessService;

    @Test
    void checkReturnsReadyStatusForHealthyParisTrip() {
        var result = readinessService.check("customer-001", "trip-paris");
        assertThat(result.score()).isGreaterThanOrEqualTo(50);
        assertThat(result.checks().stream().filter(c -> !c.passed()).count()).isLessThan(result.checks().size());
    }

    @Test
    void checkReturnsFailuresForExpiredCardTrip() {
        var result = readinessService.check("customer-001", "trip-expired-card");
        assertThat(result.checks().stream().anyMatch(c -> !c.passed())).isTrue();
        assertThat(result.score()).isLessThan(100);
    }

    @Test
    void getReturnsStoredAssessmentAfterCheck() {
        readinessService.check("customer-001", "trip-paris");
        var stored = readinessService.get("customer-001", "trip-paris");
        assertThat(stored.tripId()).isEqualTo("trip-paris");
        assertThat(stored.checks()).isNotEmpty();
    }

    @Test
    void currencyFallbackAcceptsValidOptions() {
        readinessService.setCurrencyFallback("customer-001", "trip-paris", "USD_SETTLEMENT");
        readinessService.setCurrencyFallback("customer-001", "trip-paris", "ATM_CASH");
    }

    @Test
    void currencyFallbackRejectsInvalidOption() {
        assertThatThrownBy(() -> readinessService.setCurrencyFallback("customer-001", "trip-paris", "INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getWithoutCheckThrowsNotFound() {
        assertThatThrownBy(() -> readinessService.get("customer-001", "trip-reykjavik-demo"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
