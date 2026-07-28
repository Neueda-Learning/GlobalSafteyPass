package com.travelassistant.service;

import com.travelassistant.model.Enums.ActionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentFailureService")
class PaymentFailureServiceTest {
    private final PaymentFailureService service = new PaymentFailureService();

    @Test @DisplayName("explains limit exceeded")
    void explainsLimitWithoutExposingTechnicalDetails() {
        var result = service.explain("txn-1", "LIMIT_EXCEEDED");
        assertThat(result.title()).isEqualTo("Payment limit reached");
        assertThat(result.actionType()).isEqualTo(ActionType.INCREASE_LIMIT);
    }

    @Test @DisplayName("handles unknown failure code")
    void handlesUnknownFailure() {
        assertThat(service.explain("txn-2", "SOMETHING_NEW").actionType()).isEqualTo(ActionType.CONTACT_BANK);
    }

    @Test @DisplayName("null code treated as unknown")
    void nullCodeTreatedAsUnknown() {
        var result = service.explain("txn-3", null);
        assertThat(result.failureCode()).isEqualTo("UNKNOWN");
        assertThat(result.title()).isEqualTo("Payment declined");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "INSUFFICIENT_FUNDS, Not enough funds, ADD_FUNDS",
            "CARD_FROZEN, Card is frozen, UNFREEZE_CARD",
            "CARD_EXPIRED, Card expired, USE_ANOTHER_CARD",
            "OVERSEAS_DISABLED, Overseas payments disabled, ENABLE_OVERSEAS_PAYMENT",
            "FRAUD_BLOCK, Payment needs confirmation, CONFIRM_TRANSACTION",
            "NETWORK_ERROR, Connection problem, RETRY",
            "DO_NOT_HONOR, Payment not approved, CONTACT_BANK"
    })
    @DisplayName("known failure codes")
    void knownFailureCodes(String code, String title, ActionType action) {
        var result = service.explain("txn-x", code);
        assertThat(result.title()).isEqualTo(title);
        assertThat(result.actionType()).isEqualTo(action);
    }

    @Test @DisplayName("ATM limit exceeded")
    void atmLimitExceeded() {
        var result = service.explain("txn-atm", "ATM_LIMIT_EXCEEDED");
        assertThat(result.title()).isEqualTo("ATM limit reached");
        assertThat(result.actionType()).isEqualTo(ActionType.INCREASE_LIMIT);
    }

    @Test @DisplayName("merchant not supported")
    void merchantNotSupported() {
        var result = service.explain("txn-m", "MERCHANT_NOT_SUPPORTED");
        assertThat(result.actionType()).isEqualTo(ActionType.USE_ANOTHER_CARD);
    }

    @Test @DisplayName("invalid pin")
    void invalidPin() {
        var result = service.explain("txn-pin", "INVALID_PIN");
        assertThat(result.actionType()).isEqualTo(ActionType.RETRY);
    }
}
