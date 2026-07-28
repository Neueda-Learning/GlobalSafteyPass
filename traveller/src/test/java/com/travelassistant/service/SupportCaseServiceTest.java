package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.model.*;
import com.travelassistant.repository.SupportCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SupportCaseService")
class SupportCaseServiceTest {
    private SupportCaseRepository cases;
    private TransactionService transactions;
    private SupportCaseService service;

    @BeforeEach
    void setUp() {
        cases = mock(SupportCaseRepository.class);
        transactions = mock(TransactionService.class);
        service = new SupportCaseService(cases, transactions);
    }

    @Test @DisplayName("opens payment support case")
    void opensPaymentSupportCase() {
        TravelTransaction txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setStatus(Enums.TransactionStatus.DECLINED);
        when(transactions.owned("customer-001", "txn-1")).thenReturn(txn);
        when(cases.findFirstByCustomerIdAndTransactionIdOrderByCreatedAtDesc("customer-001", "txn-1"))
                .thenReturn(Optional.empty());
        when(cases.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.openPaymentCase("customer-001", "txn-1");
        assertThat(result.type()).isEqualTo(Enums.CaseType.PAYMENT_SUPPORT);
        assertThat(result.status()).isEqualTo(Enums.CaseStatus.SUBMITTED);
    }

    @Test @DisplayName("opens fraud investigation case")
    void opensFraudInvestigationCase() {
        TravelTransaction txn = TestFixtures.purchase("txn-1", "customer-001", "trip-1");
        txn.setFraudCaseReference("CASE-ABC12345");
        when(cases.findFirstByCustomerIdAndTransactionIdOrderByCreatedAtDesc("customer-001", "txn-1"))
                .thenReturn(Optional.empty());
        when(cases.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.openFraudCase("customer-001", txn);
        assertThat(result.type()).isEqualTo(Enums.CaseType.FRAUD_INVESTIGATION);
        assertThat(result.id()).isEqualTo("CASE-ABC12345");
    }
}
