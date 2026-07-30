package com.travelassistant.service;

import com.travelassistant.model.Enums;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class AlertServiceIntegrationTest {
    @Autowired AlertService alertService;

    @Test
    void listAndGetAlerts() {
        var alerts = alertService.list("customer-001");
        assertThat(alerts).isNotEmpty();
        var first = alertService.get("customer-001", alerts.get(0).id());
        assertThat(first.id()).isEqualTo(alerts.get(0).id());
    }

    @Test
    void confirmOpenAlertUpdatesStatus() {
        var confirmed = alertService.confirm("customer-001", "alert-high-value-confirm");
        assertThat(confirmed.status()).isEqualTo(Enums.AlertStatus.CONFIRMED_SAFE);
        assertThat(confirmed.customerResponse()).isEqualTo(Enums.CustomerResponse.CONFIRM);
    }

    @Test
    void getAlertReturnsDetailsForOpenAlert() {
        var alert = alertService.get("customer-001", "alert-location-mismatch");
        assertThat(alert.status()).isEqualTo(Enums.AlertStatus.OPEN);
        assertThat(alert.merchantName()).isNotBlank();
    }

    @Test
    void confirmRejectsNonExistentAlert() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> alertService.confirm("customer-001", "alert-does-not-exist"))
                .isInstanceOf(com.travelassistant.exception.ResourceNotFoundException.class);
    }
}
