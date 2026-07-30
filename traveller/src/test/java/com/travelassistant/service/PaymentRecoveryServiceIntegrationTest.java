package com.travelassistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class PaymentRecoveryServiceIntegrationTest {
    @Autowired PaymentRecoveryService recoveryService;

    @Test
    void getRecoveryForDeclinedTransaction() {
        var recovery = recoveryService.get("customer-001", "txn-declined");
        assertThat(recovery.transactionId()).isEqualTo("txn-declined");
        assertThat(recovery.checks()).hasSize(4);
        assertThat(recovery.timeline()).isNotEmpty();
    }

    @Test
    void retryDeclinedTransactionUpdatesRecoveryState() {
        var result = recoveryService.retry("customer-001", "txn-declined");
        assertThat(result.status()).isNotNull();
        assertThat(result.reasonClassification()).isEqualTo("LIMIT_EXCEEDED");
    }

    @Test
    void useAlternateCardRequiresDifferentCard() {
        assertThatThrownBy(() -> recoveryService.useAlternateCard("customer-001", "txn-declined", "card-002"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
