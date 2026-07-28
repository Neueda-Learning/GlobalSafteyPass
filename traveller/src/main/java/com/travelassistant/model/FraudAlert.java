package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "fraud_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FraudAlert {
    @Id private String id;
    private String customerId;
    private String tripId;
    private String transactionId;
    private String cardId;
    private int riskScore;
    @Enumerated(EnumType.STRING) private Enums.RiskLevel riskLevel;
    @Enumerated(EnumType.STRING) private Enums.FraudDecision decision;
    @Column(length=1000) private String reasonCodes;
    @Column(length=1000) private String customerMessage;
    @Enumerated(EnumType.STRING) private Enums.AlertStatus status;
    private Instant createdAt;
    private Instant resolvedAt;
    @Enumerated(EnumType.STRING) private Enums.CustomerResponse customerResponse;
}
