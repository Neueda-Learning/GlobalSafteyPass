package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Card {
    @Id private String id;
    @Column(nullable=false) private String customerId;
    @Column(nullable=false) private String maskedCardNumber;
    private String cardType;
    private int expiryMonth;
    private int expiryYear;
    @Enumerated(EnumType.STRING) private Enums.CardStatus status;
    private boolean overseasPaymentsEnabled;
    private boolean onlinePaymentsEnabled;
    private boolean contactlessEnabled;
    @Column(precision=19, scale=2) private BigDecimal dailyPaymentLimit;
    @Column(precision=19, scale=2) private BigDecimal dailyWithdrawalLimit;
    @Column(nullable=false) private String linkedAccountId;
    @Column(length=3) private String mainCurrency;
    @Column(length=200) private String supportedCurrencies;
}
