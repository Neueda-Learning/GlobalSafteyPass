package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.TripRequest;
import com.travelassistant.exception.*;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TripService")
class TripServiceTest {
    private TripRepository trips;
    private CardRepository cards;
    private AuditService audit;
    private TripService service;

    @BeforeEach
    void setUp() {
        trips = mock(TripRepository.class);
        cards = mock(CardRepository.class);
        audit = mock(AuditService.class);
        service = new TripService(trips, cards, audit);
    }

    private TripRequest request() {
        return new TripRequest("Japan", "Tokyo", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                new BigDecimal("3000"), "USD", "card-001");
    }

    @Test @DisplayName("creates trip successfully")
    void createsTripSuccessfully() {
        Card card = Card.builder().id("card-001").customerId("customer-001").build();
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        when(trips.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = service.create("customer-001", request());
        assertThat(response.destinationCountry()).isEqualTo("Japan");
        verify(trips).save(any(Trip.class));
        verify(audit).log(eq("customer-001"), eq("TRIP_CREATED"), eq("TRIP"), anyString(), eq("Japan"));
    }

    @Test @DisplayName("rejects end date before start date")
    void rejectsInvalidDates() {
        var bad = new TripRequest("Japan", "Tokyo", LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1),
                new BigDecimal("3000"), "USD", "card-001");
        assertThatThrownBy(() -> service.create("customer-001", bad)).isInstanceOf(InvalidTripException.class);
    }

    @Test @DisplayName("rejects card not owned by customer")
    void rejectsForeignCard() {
        Card card = Card.builder().id("card-001").customerId("other").build();
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        assertThatThrownBy(() -> service.create("customer-001", request())).isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("owned trip enforces customer")
    void ownedTripEnforcesCustomer() {
        Trip trip = Trip.builder().id("trip-1").customerId("other").build();
        when(trips.findById("trip-1")).thenReturn(Optional.of(trip));
        assertThatThrownBy(() -> service.owned("customer-001", "trip-1")).isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("plan cash exchange")
    void planCashExchange() {
        Trip trip = Trip.builder().id("trip-1").customerId("customer-001").cashExchangePlanned(false).build();
        when(trips.findById("trip-1")).thenReturn(Optional.of(trip));
        when(trips.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.planCashExchange("customer-001", "trip-1").cashExchangePlanned()).isTrue();
    }
}
