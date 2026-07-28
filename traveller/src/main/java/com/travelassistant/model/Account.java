package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
    @Id private String id;
    @Column(nullable=false) private String customerId;
    private String accountType;
    @Column(length=3) private String currency;
    @Column(precision=19, scale=2) private BigDecimal availableBalance;
    @Enumerated(EnumType.STRING) private Enums.AccountStatus status;
}
