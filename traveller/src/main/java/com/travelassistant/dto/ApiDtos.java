package com.travelassistant.dto;

import com.travelassistant.model.Enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ApiDtos {
    private ApiDtos() {}

    public record TripRequest(
            @NotBlank String destinationCountry,
            String destinationCity,
            @NotNull @FutureOrPresent LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull @DecimalMin(value="0.01") BigDecimal budget,
            @NotBlank @Pattern(regexp="[A-Za-z]{3}") String budgetCurrency,
            @NotBlank String preferredCardId) {}

    public record TripResponse(String id, String destinationCountry, String destinationCity,
            LocalDate startDate, LocalDate endDate, BigDecimal budget, String budgetCurrency,
            String preferredCardId, TripStatus status, boolean cashExchangePlanned,
            String cashExchangeMethod,BigDecimal cashExchangeAmountUsd,String cashExchangeLocation,
            Instant cashExchangePlannedAt) {}

    public record CashExchangePlanRequest(
            @NotNull @DecimalMin("20.00") BigDecimal usdAmount,
            @NotBlank String method,@NotBlank String plannedLocation) {}
    public record CashExchangePlanResponse(String tripId,String destinationCurrency,
            BigDecimal usdAmount,BigDecimal estimatedLocalAmount,BigDecimal rate,
            String method,String plannedLocation,String atmAvailability,String feeAdvice,
            Instant plannedAt) {}
    public record MapLocationResponse(String query,String label,double latitude,double longitude) {}
    public record AtmLocationResponse(String id,String name,String operator,String address,
            String openingHours,double latitude,double longitude,int distanceMeters) {}
    public record AtmSearchResponse(double latitude,double longitude,int radiusMeters,
            List<AtmLocationResponse> atms,String provider) {}

    public record TransactionEventRequest(
            @NotBlank String transactionId, String customerId, @NotBlank String cardId,
            @NotBlank String merchantName, @NotBlank String merchantCountry, String merchantCity,
            String merchantCategory, @NotNull @Positive BigDecimal originalAmount,
            @NotBlank @Pattern(regexp="[A-Za-z]{3}") String originalCurrency,
            BigDecimal billingAmount, String billingCurrency, @NotNull Instant transactionTime,
            @NotNull TransactionType transactionType, @NotNull TransactionStatus status,
            String failureCode) {}

    public record TransactionResponse(String transactionId, String tripId, String merchantName,
            String merchantCountry, String merchantCategory, BigDecimal originalAmount,
            String originalCurrency, BigDecimal billingAmount, String billingCurrency,
            BigDecimal exchangeRate, Instant transactionTime, TransactionType transactionType,
            TransactionStatus status, String failureCode, RecoveryStatus recoveryStatus) {}

    public record LimitRequest(@NotNull @DecimalMin("0.01") BigDecimal newLimit) {}
    public record ListResponse<T>(List<T> data, int count) {}
    public record ReadinessRuleResult(String ruleCode, boolean passed, Severity severity,
            int scoreImpact, String message, String recommendedAction) {}
    public record ReadinessResponse(String tripId, int score, ReadinessStatus status,
            List<ReadinessRuleResult> checks) {}
    public record ExchangeRateQuote(String sourceCurrency, String targetCurrency,
            BigDecimal rate, LocalDate rateDate, String provider, boolean estimated) {}
    public record FailureExplanation(String transactionId, String failureCode, String title,
            String customerMessage, String recommendedAction, ActionType actionType) {}
    public record FraudRuleResult(String ruleCode, boolean triggered, int riskPoints,
            RiskLevel riskLevel, String customerReason, String internalReason) {}
    public record ExternalFraudResult(Integer riskScore, List<String> signals,
            String providerReference, String providerName) {
        public boolean available() { return riskScore != null; }
    }
    public record FraudAssessment(int internalScore, Integer externalScore, int bankProfileScore, int finalScore,
            RiskLevel riskLevel, FraudDecision decision, List<String> reasons) {}
    public record LiveRateResponse(String baseCurrency, String quoteCurrency, BigDecimal rate,
            LocalDate rateDate, String provider, boolean estimated, Instant retrievedAt,
            String disclaimer) {}
    public record JourneyExchangeRateResponse(String tripId, String destinationCountry,
            String cardId, String maskedCardNumber, String cardCurrency, String destinationCurrency,
            BigDecimal rate, LocalDate rateDate, String provider, boolean estimated, Instant retrievedAt,
            boolean destinationCurrencyVerified,boolean cardSupportsCurrency,String recommendation) {}
    public record DashboardResponse(String tripId, String destination, BigDecimal budget,
            String currency, BigDecimal spent, BigDecimal remaining, BigDecimal budgetUsagePercentage,
            int transactionCount, int approvedTransactionCount, int declinedTransactionCount,
            Map<String,BigDecimal> categorySpending, List<TransactionResponse> recentTransactions,
            ReadinessStatus readinessStatus, long activeFraudAlerts) {}
    public record CardResponse(String id, String maskedCardNumber, String cardType,
            CardStatus status, boolean overseasPaymentsEnabled, boolean onlinePaymentsEnabled,
            BigDecimal dailyPaymentLimit, BigDecimal dailyWithdrawalLimit,String mainCurrency,
            List<String> supportedCurrencies,int expiryMonth,int expiryYear) {}
    public record AlertResponse(String id, String transactionId, String cardId, int riskScore,
            RiskLevel riskLevel, FraudDecision decision, List<String> reasonCodes,
            String customerMessage, AlertStatus status, Instant createdAt, CustomerResponse customerResponse,
            String merchantName,String merchantCountry,BigDecimal amount,String currency,
            Instant transactionTime,String maskedCardNumber,String caseReference) {}
    public record SupportCaseResponse(String id,String tripId,String transactionId,CaseType type,
            CaseStatus status,String title,String currentUpdate,Instant createdAt,Instant updatedAt) {}
    public record RecoveryCheck(String label,boolean passed,String detail) {}
    public record RecoveryTimelineEvent(Instant at,String title,String detail,String state) {}
    public record AlternateCardRecoveryRequest(@NotBlank String cardId) {}
    public record RecoveryCardOption(String cardId,String maskedCardNumber,String cardType,
            String mainCurrency,List<String> supportedCurrencies,String recommendation) {}
    public record PaymentRecoveryResponse(String transactionId,RecoveryStatus status,String merchantName,
            BigDecimal amount,String currency,String merchantCity,String merchantCountry,String maskedCardNumber,
            String reasonClassification,int confidence,String explanation,String safetyMessage,String travelContext,
            String recommendedAction,List<String> alternativeActions,List<RecoveryCardOption> eligibleCards,List<RecoveryCheck> checks,
            List<RecoveryTimelineEvent> timeline) {}
        public record CountryOption(String name,String iso2,String iso3,String mainstreamCurrency) {}
        public record CityOption(String name) {}
        public record CurrencyOption(String code,String name,boolean mainstreamForCountry) {}
}
