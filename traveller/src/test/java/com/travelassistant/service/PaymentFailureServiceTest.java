package com.travelassistant.service;
import com.travelassistant.model.Enums.ActionType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class PaymentFailureServiceTest {
    private final PaymentFailureService service=new PaymentFailureService();
    @Test void explainsLimitWithoutExposingTechnicalDetails(){
        var result=service.explain("txn-1","LIMIT_EXCEEDED");
        assertThat(result.title()).isEqualTo("Payment limit reached");
        assertThat(result.actionType()).isEqualTo(ActionType.INCREASE_LIMIT);
    }
    @Test void handlesUnknownFailure(){
        assertThat(service.explain("txn-2","SOMETHING_NEW").actionType()).isEqualTo(ActionType.CONTACT_BANK);
    }
}
