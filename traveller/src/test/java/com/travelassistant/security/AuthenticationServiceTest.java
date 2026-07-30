package com.travelassistant.security;

import com.travelassistant.exception.UnauthorizedException;
import com.travelassistant.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
class AuthenticationServiceTest {
    @Autowired AuthenticationService auth;
    @Autowired CustomerRepository customers;

    @Test
    void fullAuthenticationFlowWithTrustedDevice() {
        var start = auth.start("Jessie Han");
        assertThat(start.challengeId()).isNotBlank();
        var verify = auth.verify(new AuthDtos.VerifyRequest(start.challengeId(), AuthDtos.Method.TRUSTED_DEVICE, "trusted-device-demo"));
        assertThat(verify.customerId()).isEqualTo("customer-001");
        assertThat(auth.requireSession(verify.accessToken()).customerId()).isEqualTo("customer-001");
        auth.logout(verify.accessToken());
        assertThatThrownBy(() -> auth.requireSession(verify.accessToken()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void unknownCustomerCannotStart() {
        assertThatThrownBy(() -> auth.start("Unknown Person"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void smsOtpRequiresSmsSentFirst() {
        var start = auth.start("Milly Li");
        assertThatThrownBy(() -> auth.verify(new AuthDtos.VerifyRequest(start.challengeId(), AuthDtos.Method.SMS_OTP, "246810")))
                .isInstanceOf(UnauthorizedException.class);
        auth.sendSms(start.challengeId());
        var verify = auth.verify(new AuthDtos.VerifyRequest(start.challengeId(), AuthDtos.Method.SMS_OTP, "246810"));
        assertThat(verify.customerId()).isEqualTo("customer-002");
    }

    @Test
    void stepUpTokenIsSingleUse() {
        var stepUp = auth.stepUp("customer-001", new AuthDtos.StepUpRequest("CARD_FREEZE", "card-001", AuthDtos.Method.TRUSTED_DEVICE, "trusted-device-demo"));
        auth.consumeStepUp(stepUp.stepUpToken(), "customer-001", "CARD_FREEZE", "card-001");
        assertThatThrownBy(() -> auth.consumeStepUp(stepUp.stepUpToken(), "customer-001", "CARD_FREEZE", "card-001"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
