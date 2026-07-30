package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.CashExchangePlanRequest;
import com.travelassistant.exception.ForbiddenException;
import com.travelassistant.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class CashExchangePlanningServiceIntegrationTest {
    @Autowired CashExchangePlanningService cashExchangePlanningService;
    @Autowired TripRepository tripRepository;

    @Test
    void savePlanForMappedDestinationCalculatesLocalAmount() {
        var request = new CashExchangePlanRequest(new BigDecimal("200.00"), "ATM_CASH", "Charles de Gaulle Airport");
        var response = cashExchangePlanningService.save("customer-001", "trip-paris", request);
        assertThat(response.destinationCurrency()).isEqualTo("EUR");
        assertThat(response.estimatedLocalAmount()).isNotNull();
        assertThat(response.rate()).isNotNull();
        var trip = tripRepository.findById("trip-paris").orElseThrow();
        assertThat(trip.isCashExchangePlanned()).isTrue();
    }

    @Test
    void savePlanForUnmappedDestinationLeavesAmountNull() {
        var request = new CashExchangePlanRequest(new BigDecimal("100.00"), "ATM_CASH", "Keflavik Airport");
        var response = cashExchangePlanningService.save("customer-001", "trip-reykjavik-demo", request);
        assertThat(response.destinationCurrency()).isNull();
        assertThat(response.estimatedLocalAmount()).isNull();
        assertThat(tripRepository.findById("trip-reykjavik-demo").orElseThrow().isCashExchangePlanned()).isTrue();
    }

    @Test
    void savePlanRejectsAnotherCustomersTrip() {
        var request = new CashExchangePlanRequest(new BigDecimal("100.00"), "ATM_CASH", "Airport");
        assertThatThrownBy(() -> cashExchangePlanningService.save("customer-002", "trip-paris", request))
                .isInstanceOf(ForbiddenException.class);
    }
}
