package com.travelassistant.service;

import com.travelassistant.model.Enums.ActionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFailureServiceTest {
    private final PaymentFailureService service = new PaymentFailureService();

    @ParameterizedTest
    @CsvSource({
            "LIMIT_EXCEEDED,Payment limit reached,INCREASE_LIMIT",
            "INSUFFICIENT_FUNDS,Not enough funds,ADD_FUNDS",
            "CARD_FROZEN,Card is frozen,UNFREEZE_CARD",
            "OVERSEAS_DISABLED,Overseas payments disabled,ENABLE_OVERSEAS_PAYMENT",
            "FRAUD_BLOCK,Payment needs confirmation,CONFIRM_TRANSACTION",
            "NETWORK_ERROR,Connection problem,RETRY",
            "INVALID_PIN,PIN was not accepted,RETRY"
    })
    void explainsKnownFailureCodes(String code, String title, ActionType action) {
        var result = service.explain("txn-1", code);
        assertThat(result.title()).isEqualTo(title);
        assertThat(result.actionType()).isEqualTo(action);
        assertThat(result.transactionId()).isEqualTo("txn-1");
    }

    @Test
    void handlesUnknownFailure() {
        assertThat(service.explain("txn-2", "SOMETHING_NEW").actionType()).isEqualTo(ActionType.CONTACT_BANK);
    }

    @Test
    void nullFailureCodeUsesUnknownTemplate() {
        assertThat(service.explain("txn-3", null).failureCode()).isEqualTo("UNKNOWN");
    }
}
