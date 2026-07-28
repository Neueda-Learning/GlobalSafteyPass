package com.globalsafetypass.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trips")
public class Trip {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private User user;
    @ManyToOne(optional = false) private Card preferredCard;
    @Column(nullable = false) private String destinationCountry;
    @Column(nullable = false) private String destinationCity;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal budget;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal spent = BigDecimal.ZERO;

    public Trip() {}
    public Trip(User user, Card card, String country, String city, LocalDate startDate,
                LocalDate endDate, BigDecimal budget, String currency) {
        this.user = user; this.preferredCard = card; this.destinationCountry = country;
        this.destinationCity = city; this.startDate = startDate; this.endDate = endDate;
        this.budget = budget; this.currency = currency; this.spent = BigDecimal.ZERO;
    }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Card getPreferredCard() { return preferredCard; }
    public String getDestinationCountry() { return destinationCountry; }
    public String getDestinationCity() { return destinationCity; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getBudget() { return budget; }
    public String getCurrency() { return currency; }
    public BigDecimal getSpent() { return spent; }
    public BigDecimal getRemaining() { return budget.subtract(spent); }
    public void addSpend(BigDecimal amount) { this.spent = this.spent.add(amount); }
}

