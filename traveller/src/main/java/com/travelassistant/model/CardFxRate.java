package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name="card_fx_rates",uniqueConstraints=@UniqueConstraint(columnNames={"card_id","target_currency"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardFxRate {
    @Id private String id;
    @Column(name="card_id",nullable=false) private String cardId;
    @Column(name="source_currency",nullable=false,length=3) private String sourceCurrency;
    @Column(name="target_currency",nullable=false,length=3) private String targetCurrency;
    @Column(nullable=false,precision=19,scale=8) private BigDecimal rate;
    private String provider;
    private boolean estimated;
    private Instant updatedAt;
}
