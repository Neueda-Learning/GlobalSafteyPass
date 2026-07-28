package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("AlertService")
class AlertServiceTest {
    private FraudAlertRepository alerts;
    private TransactionRepository txs;
    private CardControlService cards;
    private AuditService audit;
    private SupportCaseService cases;
    private AlertService service;

    @BeforeEach
    void setUp() {
        alerts = mock(FraudAlertRepository.class);
        txs = mock(TransactionRepository.class);
        cards = mock(CardControlService.class);
        audit = mock(AuditService.class);
        cases = mock(SupportCaseService.class);
        service = new AlertService(alerts, txs, cards, audit, cases);
    }

    @Test @DisplayName("confirm alert marks safe")
    void confirmAlertMarksSafe() {
        FraudAlert alert = TestFixtures.openAlert("alert-1", "customer-001", "txn-1");
        when(alerts.findById("alert-1")).thenReturn(Optional.of(alert));
        when(alerts.save(any())).thenAnswer(i -> i.getArgument(0));
        TravelTransaction txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        when(txs.findById("txn-1")).thenReturn(Optional.of(txn));
        when(cards.owned("customer-001", "card-001")).thenReturn(TestFixtures.activeCard("card-001", "customer-001"));
        var result = service.confirm("customer-001", "alert-1");
        assertThat(result.status()).isEqualTo(Enums.AlertStatus.CONFIRMED_SAFE);
        verify(audit).log("customer-001", "TRANSACTION_CONFIRMED", "ALERT", "alert-1", "Safe");
    }

    @Test @DisplayName("list alerts for customer")
    void listAlertsForCustomer() {
        FraudAlert alert = TestFixtures.openAlert("alert-1", "customer-001", "txn-1");
        when(alerts.findByCustomerIdOrderByCreatedAtDesc("customer-001")).thenReturn(List.of(alert));
        when(txs.findById("txn-1")).thenReturn(Optional.of(TestFixtures.purchase("txn-1", "customer-001", "trip-1")));
        when(cards.owned("customer-001", "card-001")).thenReturn(TestFixtures.activeCard("card-001", "customer-001"));
        assertThat(service.list("customer-001")).hasSize(1);
    }
}
