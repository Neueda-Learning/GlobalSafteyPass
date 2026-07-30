package com.travelassistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class TravelDashboardServiceIntegrationTest {
    @Autowired TravelDashboardService dashboardService;

    @Test
    void dashboardAggregatesTripSpending() {
        var dashboard = dashboardService.get("customer-001", "trip-paris");
        assertThat(dashboard.tripId()).isEqualTo("trip-paris");
        assertThat(dashboard.budget()).isEqualByComparingTo(new BigDecimal("3200"));
        assertThat(dashboard.transactionCount()).isGreaterThanOrEqualTo(0);
    }
}
