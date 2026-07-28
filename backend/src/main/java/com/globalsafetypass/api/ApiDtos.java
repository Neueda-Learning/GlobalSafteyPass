package com.globalsafetypass.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public record TripRequest(
        @NotBlank String destinationCountry,
        @NotBlank String destinationCity,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin("0.01") BigDecimal budget,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull Long cardId
    ) {}

    public record CardUpdate(
        Boolean overseasEnabled,
        Boolean frozen,
        @DecimalMin("0.00") BigDecimal balance,
        @DecimalMin("0.01") BigDecimal singleLimit,
        @DecimalMin("0.01") BigDecimal dailyLimit
    ) {}

    public record ReadinessItem(String key, String label, boolean passed, int points,
                                String message, String action) {}
    public record ReadinessResult(int score, String status, List<ReadinessItem> items) {}

    public record BankEvent(
        @NotBlank String provider,
        @NotBlank String externalTransactionId,
        @NotBlank String eventId,
        @NotNull Long cardId,
        @NotBlank String merchant,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @DecimalMin("0.000001") BigDecimal exchangeRate,
        @NotBlank String country,
        @NotBlank String city,
        @NotBlank String category,
        @NotBlank String channel,
        @NotNull LocalDateTime occurredAt
    ) {}

    public record AlertAction(@NotBlank String action, String reason, String notes) {}
    public record IngestionResult(boolean duplicate, Long transactionId, String status,
                                  String message, String recommendedAction) {}
}

