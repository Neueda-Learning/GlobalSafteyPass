package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.TripRequest;
import com.travelassistant.exception.ForbiddenException;
import com.travelassistant.exception.InvalidTripException;
import com.travelassistant.exception.ResourceNotFoundException;
import com.travelassistant.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class TripServiceIntegrationTest {
    @Autowired TripService tripService;
    @Autowired AuditLogRepository auditLogs;

    @Test
    void createListGetUpdateDeleteTripLifecycle() {
        var request = new TripRequest("Iceland", "Reykjavik",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 7),
                new BigDecimal("2700"), "USD", "card-001");
        var created = tripService.create("customer-001", request);
        assertThat(created.destinationCountry()).isEqualTo("Iceland");
        assertThat(tripService.list("customer-001")).anyMatch(t -> t.id().equals(created.id()));

        var updated = tripService.update("customer-001", created.id(),
                new TripRequest("Iceland", "Reykjavik",
                        LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 10),
                        new BigDecimal("3000"), "USD", "card-001"));
        assertThat(updated.budget()).isEqualByComparingTo(new BigDecimal("3000"));

        tripService.delete("customer-001", created.id());
        assertThatThrownBy(() -> tripService.get("customer-001", created.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tokyoDemoTripGetsFixedId() {
        var request = new TripRequest("Japan", "Tokyo",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5),
                new BigDecimal("2500"), "USD", "card-002");
        assertThat(tripService.create("customer-001", request).id()).isEqualTo("trip-tokyo");
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        var request = new TripRequest("France", "Paris",
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1),
                new BigDecimal("1000"), "USD", "card-001");
        assertThatThrownBy(() -> tripService.create("customer-001", request))
                .isInstanceOf(InvalidTripException.class);
    }

    @Test
    void rejectsCardOwnedByAnotherCustomer() {
        var request = new TripRequest("France", "Paris",
                LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 7),
                new BigDecimal("1000"), "USD", "card-201");
        assertThatThrownBy(() -> tripService.create("customer-001", request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void ownedReturnsTripForMatchingCustomer() {
        assertThat(tripService.get("customer-001", "trip-paris").id()).isEqualTo("trip-paris");
    }

    @Test
    void cannotDeleteCompletedTrip() {
        assertThatThrownBy(() -> tripService.delete("customer-001", "trip-singapore"))
                .isInstanceOf(InvalidTripException.class);
    }

    @Test
    void cannotAccessAnotherCustomersTrip() {
        assertThatThrownBy(() -> tripService.get("customer-002", "trip-paris"))
                .isInstanceOf(ForbiddenException.class);
    }
}
