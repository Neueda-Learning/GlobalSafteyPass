package com.globalsafetypass.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_reports")
public class FraudReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) private RiskAlert alert;
    @Column(nullable = false) private String reason;
    @Column(length = 800) private String notes;
    @Column(nullable = false) private String status = "SUBMITTED";
    @Column(nullable = false) private LocalDateTime submittedAt = LocalDateTime.now();

    public FraudReport() {}
    public FraudReport(RiskAlert alert, String reason, String notes) {
        this.alert = alert; this.reason = reason; this.notes = notes;
    }
    public Long getId() { return id; }
    public String getStatus() { return status; }
}
