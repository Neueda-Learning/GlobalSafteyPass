package travelassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(nullable = false)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
