package com.globalsafetypass.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_alerts")
public class RiskAlert {
    public enum Severity { LOW, MEDIUM, HIGH }
    public enum Status { OPEN, CONFIRMED_SAFE, REPORTED, DISMISSED, CARD_FROZEN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private BankTransaction transaction;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Severity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.OPEN;
    @Column(nullable = false) private String ruleCode;
    @Column(nullable = false) private String title;
    @Column(nullable = false, length = 600) private String reason;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public RiskAlert() {}
    public RiskAlert(BankTransaction tx, Severity severity, String rule, String title, String reason) {
        this.transaction = tx; this.severity = severity; this.ruleCode = rule;
        this.title = title; this.reason = reason;
    }
    public Long getId() { return id; }
    public BankTransaction getTransaction() { return transaction; }
    public Severity getSeverity() { return severity; }
    public Status getStatus() { return status; }
    public String getRuleCode() { return ruleCode; }
    public String getTitle() { return title; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setStatus(Status status) { this.status = status; }
}

