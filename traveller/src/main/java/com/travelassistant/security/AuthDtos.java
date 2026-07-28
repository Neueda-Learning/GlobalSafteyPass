package com.travelassistant.security;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public final class AuthDtos {
    private AuthDtos(){}
    public enum Method { TRUSTED_DEVICE, APP_PIN, SMS_OTP }
    public record StartRequest(@NotBlank String customerId){}
    public record StartResponse(String challengeId,Method preferredMethod,List<Method> availableMethods,
            String maskedPhone,Instant expiresAt,String message){}
    public record VerifyRequest(@NotBlank String challengeId,Method method,String credential){}
    public record VerifyResponse(String accessToken,String customerId,Method authenticationMethod,
            Instant expiresAt,String tokenType){}
    public record SmsResponse(String challengeId,String maskedPhone,Instant expiresAt,String demoCode){}
    public record SessionResponse(String customerId,Method authenticationMethod,Instant expiresAt){}
    public record StepUpRequest(@NotBlank String action,@NotBlank String resourceId,Method method,String credential){}
    public record StepUpResponse(String stepUpToken,String action,String resourceId,Instant expiresAt){}
}
