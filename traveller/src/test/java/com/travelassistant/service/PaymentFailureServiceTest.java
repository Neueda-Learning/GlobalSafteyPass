package com.travelassistant.service;

import com.travelassistant.model.Enums.ActionType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentFailureServiceTest {
    private final PaymentFailureService service = new PaymentFailureService();

    @Test
    void explainsPaymentLimitWithoutExposingTechnicalDetails() {
        var result = service.explain("txn-1", "LIMIT_EXCEEDED");
        assertThat(result.title()).isEqualTo("Payment limit reached");
        assertThat(result.actionType()).isEqualTo(ActionType.INCREASE_LIMIT);
        assertThat(result.customerMessage()).contains("daily card limit");
    }

    @Test
    void explainsAtmLimitExceeded() {
        var result = service.explain("txn-atm-limit", "ATM_LIMIT_EXCEEDED");
        assertThat(result.title()).isEqualTo("ATM limit reached");
        assertThat(result.failureCode()).isEqualTo("ATM_LIMIT_EXCEEDED");
        assertThat(result.actionType()).isEqualTo(ActionType.INCREASE_LIMIT);
        assertThat(result.customerMessage()).contains("ATM limit");
        assertThat(result.recommendedAction()).contains("withdrawal limit");
    }

    @Test
    void handlesUnknownFailure() {
        assertThat(service.explain("txn-2", "SOMETHING_NEW").actionType()).isEqualTo(ActionType.CONTACT_BANK);
    }

    @Test
    void handlesNullFailureCode() {
        var result = service.explain("txn-3", null);
        assertThat(result.title()).isEqualTo("Payment declined");
        assertThat(result.actionType()).isEqualTo(ActionType.CONTACT_BANK);
    }
}
