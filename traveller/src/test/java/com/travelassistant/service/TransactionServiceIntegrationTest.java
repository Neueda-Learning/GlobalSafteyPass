package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.TransactionEventRequest;
import com.travelassistant.exception.DuplicateTransactionException;
import com.travelassistant.exception.ForbiddenException;
import com.travelassistant.model.Enums;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class TransactionServiceIntegrationTest {
    @Autowired TransactionService transactionService;

    @Test
    void receiveApprovedTransactionPersistsSuccessfully() {
        String id = "txn-approved-" + UUID.randomUUID();
        var event = new TransactionEventRequest(id, null, "card-001", "Bakery", "France",
                "Paris", "DINING", new BigDecimal("12.00"), "EUR", null, null,
                Instant.parse("2026-11-06T08:00:00Z"), Enums.TransactionType.PURCHASE,
                Enums.TransactionStatus.APPROVED, null);
        var response = transactionService.receive("customer-001", event);
        assertThat(response.status()).isEqualTo(Enums.TransactionStatus.APPROVED);
        assertThat(response.failureCode()).isNull();
    }

    @Test
    void receiveDeclinedTransactionStoresFailureCode() {
        String id = "txn-declined-new-" + UUID.randomUUID();
        var event = new TransactionEventRequest(id, null, "card-001", "Shop", "France",
                "Paris", "RETAIL", new BigDecimal("30.00"), "EUR", null, null,
                Instant.parse("2026-11-06T09:00:00Z"), Enums.TransactionType.PURCHASE,
                Enums.TransactionStatus.DECLINED, "NETWORK_ERROR");
        var response = transactionService.receive("customer-001", event);
        assertThat(response.status()).isEqualTo(Enums.TransactionStatus.DECLINED);
        assertThat(response.failureCode()).isEqualTo("NETWORK_ERROR");
    }

    @Test
    void duplicateTransactionIdRejected() {
        var event = new TransactionEventRequest("txn-hotel", null, "card-002", "Hotel", "Japan",
                "Tokyo", "HOTEL", new BigDecimal("100"), "JPY", null, null,
                Instant.now(), Enums.TransactionType.PURCHASE, Enums.TransactionStatus.APPROVED, null);
        assertThatThrownBy(() -> transactionService.receive("customer-001", event))
                .isInstanceOf(DuplicateTransactionException.class);
    }

    @Test
    void failureExplanationOnlyForDeclinedTransactions() {
        assertThat(transactionService.failure("customer-001", "txn-declined").failureCode()).isEqualTo("LIMIT_EXCEEDED");
        assertThatThrownBy(() -> transactionService.failure("customer-001", "txn-hotel"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotUseAnotherCustomersCard() {
        String id = "txn-forbidden-" + UUID.randomUUID();
        var event = new TransactionEventRequest(id, null, "card-201", "Shop", "France",
                "Paris", "RETAIL", new BigDecimal("10"), "EUR", null, null,
                Instant.now(), Enums.TransactionType.PURCHASE, Enums.TransactionStatus.APPROVED, null);
        assertThatThrownBy(() -> transactionService.receive("customer-001", event))
                .isInstanceOf(ForbiddenException.class);
    }
}
