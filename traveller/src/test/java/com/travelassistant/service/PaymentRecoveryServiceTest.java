package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PaymentRecoveryService")
class PaymentRecoveryServiceTest {
    private TransactionService transactions;
    private TransactionRepository repository;
    private CardRepository cards;
    private AccountRepository accounts;
    private TripRepository trips;
    private FraudAlertRepository alerts;
    private AuditService audit;
    private PaymentRecoveryService service;

    @BeforeEach
    void setUp() {
        transactions = mock(TransactionService.class);
        repository = mock(TransactionRepository.class);
        cards = mock(CardRepository.class);
        accounts = mock(AccountRepository.class);
        trips = mock(TripRepository.class);
        alerts = mock(FraudAlertRepository.class);
        audit = mock(AuditService.class);
        var recommendations = new TravelCardRecommendationService(cards, accounts, new CardCapabilityService());
        service = new PaymentRecoveryService(transactions, repository, cards, accounts, trips, alerts, audit,
                new CardCapabilityService(), recommendations);
    }

    @Test @DisplayName("network error retry completes")
    void networkErrorRetryCompletes() {
        TravelTransaction txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setStatus(Enums.TransactionStatus.DECLINED);
        txn.setFailureCode("NETWORK_ERROR");
        txn.setRecoveryStatus(Enums.RecoveryStatus.PAYMENT_INTERRUPTED);
        when(transactions.owned("customer-001", "txn-1")).thenReturn(txn);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        Account account = TestFixtures.activeAccount("acct-001", "customer-001");
        Trip trip = TestFixtures.trip("trip-1", "customer-001");
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        when(accounts.findById("acct-001")).thenReturn(Optional.of(account));
        when(trips.findById("trip-1")).thenReturn(Optional.of(trip));
        when(alerts.findByTransactionId("txn-1")).thenReturn(List.of());
        var result = service.retry("customer-001", "txn-1");
        assertThat(result.status()).isEqualTo(Enums.RecoveryStatus.COMPLETED_AFTER_RETRY);
        assertThat(txn.getStatus()).isEqualTo(Enums.TransactionStatus.APPROVED);
    }

    @Test @DisplayName("limit exceeded retry requires action")
    void limitExceededRetryRequiresAction() {
        TravelTransaction txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setStatus(Enums.TransactionStatus.DECLINED);
        txn.setFailureCode("LIMIT_EXCEEDED");
        when(transactions.owned("customer-001", "txn-1")).thenReturn(txn);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        when(accounts.findById("acct-001")).thenReturn(Optional.of(TestFixtures.activeAccount("acct-001", "customer-001")));
        when(trips.findById("trip-1")).thenReturn(Optional.of(TestFixtures.trip("trip-1", "customer-001")));
        when(alerts.findByTransactionId("txn-1")).thenReturn(List.of());
        var result = service.retry("customer-001", "txn-1");
        assertThat(result.status()).isEqualTo(Enums.RecoveryStatus.ACTION_REQUIRED);
    }

    @Test @DisplayName("alternate card recovery")
    void alternateCardRecovery() {
        TravelTransaction txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setStatus(Enums.TransactionStatus.DECLINED);
        txn.setFailureCode("INSUFFICIENT_FUNDS");
        when(transactions.owned("customer-001", "txn-1")).thenReturn(txn);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        Card backup = TestFixtures.activeCard("card-002", "customer-001");
        when(cards.findById("card-002")).thenReturn(Optional.of(backup));
        when(cards.findById("card-001")).thenReturn(Optional.of(TestFixtures.activeCard("card-001", "customer-001")));
        when(accounts.findById("acct-001")).thenReturn(Optional.of(TestFixtures.activeAccount("acct-001", "customer-001")));
        when(trips.findById("trip-1")).thenReturn(Optional.of(TestFixtures.trip("trip-1", "customer-001")));
        when(alerts.findByTransactionId("txn-1")).thenReturn(List.of());
        var result = service.useAlternateCard("customer-001", "txn-1", "card-002");
        assertThat(result.status()).isEqualTo(Enums.RecoveryStatus.COMPLETED_WITH_ALTERNATE_CARD);
    }

    @Test @DisplayName("rejects same alternate card")
    void rejectsSameAlternateCard() {
        TravelTransaction txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        when(transactions.owned("customer-001", "txn-1")).thenReturn(txn);
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        assertThatThrownBy(() -> service.useAlternateCard("customer-001", "txn-1", "card-001"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
