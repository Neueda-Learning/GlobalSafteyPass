package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name = "trips")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Trip {
    @Id private String id;
    @Column(nullable=false) private String customerId;
    @Column(nullable=false) private String destinationCountry;
    private String destinationCity;
    @Column(nullable=false) private LocalDate startDate;
    @Column(nullable=false) private LocalDate endDate;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal budget;
    @Column(nullable=false, length=3) private String budgetCurrency;
    @Column(nullable=false) private String preferredCardId;
    @Enumerated(EnumType.STRING) private Enums.TripStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean cashExchangePlanned;
    private String cashExchangeMethod;
    @Column(precision=19, scale=2) private BigDecimal cashExchangeAmountUsd;
    private String cashExchangeLocation;
    private Instant cashExchangePlannedAt;
}
