package com.travelassistant.security;

import com.travelassistant.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static com.travelassistant.security.AuthDtos.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("AuthenticationService")
class AuthenticationServiceTest {
    private AuthenticationService auth;

    @BeforeEach
    void setUp() { auth = new AuthenticationService(); }

    @Test @DisplayName("start auth for valid customer")
    void startAuthForValidCustomer() {
        StartResponse response = auth.start("customer-001");
        assertThat(response.challengeId()).isNotBlank();
        assertThat(response.preferredMethod()).isEqualTo(Method.TRUSTED_DEVICE);
    }

    @Test @DisplayName("rejects unknown customer")
    void startAuthRejectsUnknownCustomer() {
        assertThatThrownBy(() -> auth.start("unknown")).isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("trusted device verification")
    void trustedDeviceVerification() {
        String id = auth.start("customer-001").challengeId();
        VerifyResponse r = auth.verify(new VerifyRequest(id, Method.TRUSTED_DEVICE, "trusted-device-demo"));
        assertThat(r.accessToken()).isNotBlank();
        assertThat(r.customerId()).isEqualTo("customer-001");
    }

    @Test @DisplayName("app pin verification")
    void appPinVerification() {
        String id = auth.start("customer-002").challengeId();
        assertThat(auth.verify(new VerifyRequest(id, Method.APP_PIN, "2580")).customerId()).isEqualTo("customer-002");
    }

    @Test @DisplayName("sms requires send first")
    void smsRequiresSendFirst() {
        String id = auth.start("customer-001").challengeId();
        assertThatThrownBy(() -> auth.verify(new VerifyRequest(id, Method.SMS_OTP, "246810")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("sms otp after send")
    void smsOtpAfterSend() {
        String id = auth.start("customer-001").challengeId();
        auth.sendSms(id);
        assertThat(auth.verify(new VerifyRequest(id, Method.SMS_OTP, "246810")).accessToken()).isNotBlank();
    }

    @Test @DisplayName("wrong credential rejected")
    void wrongCredentialRejected() {
        String id = auth.start("customer-001").challengeId();
        assertThatThrownBy(() -> auth.verify(new VerifyRequest(id, Method.APP_PIN, "0000")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("session valid after login")
    void sessionValidAfterLogin() {
        String token = auth.verify(new VerifyRequest(auth.start("customer-001").challengeId(),
                Method.TRUSTED_DEVICE, "trusted-device-demo")).accessToken();
        assertThat(auth.requireSession(token).customerId()).isEqualTo("customer-001");
    }

    @Test @DisplayName("logout invalidates session")
    void logoutInvalidatesSession() {
        String token = auth.verify(new VerifyRequest(auth.start("customer-001").challengeId(),
                Method.TRUSTED_DEVICE, "trusted-device-demo")).accessToken();
        auth.logout(token);
        assertThatThrownBy(() -> auth.requireSession(token)).isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("step-up grant flow")
    void stepUpGrantFlow() {
        StepUpResponse s = auth.stepUp("customer-001",
                new StepUpRequest("FREEZE_CARD", "card-001", Method.TRUSTED_DEVICE, "trusted-device-demo"));
        auth.consumeStepUp(s.stepUpToken(), "customer-001", "FREEZE_CARD", "card-001");
    }
}
