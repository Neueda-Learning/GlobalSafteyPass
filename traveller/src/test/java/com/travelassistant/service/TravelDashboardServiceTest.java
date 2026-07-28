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

@DisplayName("TravelDashboardService")
class TravelDashboardServiceTest {
    private TripService trips;
    private TransactionRepository txs;
    private TransactionService mapper;
    private ReadinessAssessmentRepository readiness;
    private FraudAlertRepository alerts;
    private TravelDashboardService service;

    @BeforeEach
    void setUp() {
        trips = mock(TripService.class);
        txs = mock(TransactionRepository.class);
        mapper = mock(TransactionService.class);
        readiness = mock(ReadinessAssessmentRepository.class);
        alerts = mock(FraudAlertRepository.class);
        service = new TravelDashboardService(trips, txs, mapper, readiness, alerts);
    }

    @Test @DisplayName("calculates spent and remaining budget")
    void calculatesSpentAndRemaining() {
        Trip trip = TestFixtures.trip("trip-1", "customer-001");
        when(trips.owned("customer-001", "trip-1")).thenReturn(trip);
        TravelTransaction approved = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        approved.setBillingAmount(new BigDecimal("500"));
        TravelTransaction declined = TestFixtures.purchase("txn-2", "customer-001", "trip-1");
        declined.setStatus(Enums.TransactionStatus.DECLINED);
        when(txs.findByTripIdOrderByTransactionTimeDesc("trip-1")).thenReturn(List.of(approved, declined));
        when(readiness.findByTripId("trip-1")).thenReturn(Optional.empty());
        when(alerts.countByTripIdAndStatus("trip-1", Enums.AlertStatus.OPEN)).thenReturn(0L);
        var dash = service.get("customer-001", "trip-1");
        assertThat(dash.spent()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(dash.remaining()).isEqualByComparingTo(new BigDecimal("2500"));
        assertThat(dash.approvedTransactionCount()).isEqualTo(1);
        assertThat(dash.declinedTransactionCount()).isEqualTo(1);
    }
}
