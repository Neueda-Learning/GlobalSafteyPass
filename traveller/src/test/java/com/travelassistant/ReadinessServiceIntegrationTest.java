package com.travelassistant;

import com.travelassistant.dto.ApiDtos.TripRequest;
import com.travelassistant.service.ReadinessService;
import com.travelassistant.service.TripService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false"})
@ActiveProfiles("test")
@DisplayName("ReadinessService Integration")
class ReadinessServiceIntegrationTest {
    @Autowired TripService tripService;
    @Autowired ReadinessService readinessService;

    @Test @DisplayName("readiness check on H2 with seeded data")
    void readinessCheckOnH2() {
        var trip = tripService.create("customer-001", new TripRequest(
                "Japan", "Tokyo", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10),
                new BigDecimal("2000"), "USD", "card-001"));
        var result = readinessService.check("customer-001", trip.id());
        assertThat(result.score()).isBetween(0, 100);
        assertThat(result.checks()).isNotEmpty();
        assertThat(readinessService.get("customer-001", trip.id()).score()).isEqualTo(result.score());
    }
}
