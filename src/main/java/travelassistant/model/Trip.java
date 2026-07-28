package travelassistant.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false)
    private String currency;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "preferred_card_id")
    private Card preferredCard;

    @Column(nullable = false)
    private int readinessScore = 0;

    @Column(nullable = false)
    private String status = "PLANNED";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Card getPreferredCard() { return preferredCard; }
    public void setPreferredCard(Card preferredCard) { this.preferredCard = preferredCard; }
    public int getReadinessScore() { return readinessScore; }
    public void setReadinessScore(int readinessScore) { this.readinessScore = readinessScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
