package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "travel_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TravelTransaction {
    @Id private String transactionId;
    @Column(nullable=false) private String customerId;
    private String tripId;
    @Column(nullable=false) private String cardId;
    private String merchantName;
    private String merchantCountry;
    private String merchantCity;
    private String merchantCategory;
    @Column(precision=19, scale=4) private BigDecimal originalAmount;
    @Column(length=3) private String originalCurrency;
    @Column(precision=19, scale=4) private BigDecimal billingAmount;
    @Column(length=3) private String billingCurrency;
    @Column(precision=19, scale=8) private BigDecimal exchangeRate;
    private Instant transactionTime;
    @Enumerated(EnumType.STRING) private Enums.TransactionType transactionType;
    @Enumerated(EnumType.STRING) private Enums.TransactionStatus status;
    private String failureCode;
    private boolean disputed;
    private String fraudCaseReference;
    @Enumerated(EnumType.STRING) @Column(columnDefinition="varchar(40)") private Enums.RecoveryStatus recoveryStatus;
    private int recoveryAttemptCount;
    private Instant recoveryStartedAt;
    private Instant recoveryCompletedAt;
    private Instant recoveryUpdatedAt;
    private Instant createdAt;
}
