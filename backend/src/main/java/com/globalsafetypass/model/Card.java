package com.globalsafetypass.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "cards")
public class Card {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private User user;
    @Column(nullable = false) private String nickname;
    @Column(nullable = false) private String network;
    @Column(nullable = false, length = 4) private String lastFour;
    @Column(nullable = false) private String expiry;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal balance;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal singleLimit;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal dailyLimit;
    @Column(nullable = false) private boolean overseasEnabled;
    @Column(nullable = false) private boolean frozen;
    @Column(nullable = false, length = 3) private String currency;

    public Card() {}
    public Card(User user, String nickname, String network, String lastFour, String expiry,
                BigDecimal balance, BigDecimal singleLimit, BigDecimal dailyLimit,
                boolean overseasEnabled, boolean frozen, String currency) {
        this.user = user; this.nickname = nickname; this.network = network;
        this.lastFour = lastFour; this.expiry = expiry; this.balance = balance;
        this.singleLimit = singleLimit; this.dailyLimit = dailyLimit;
        this.overseasEnabled = overseasEnabled; this.frozen = frozen; this.currency = currency;
    }
    public boolean isExpiredAt(YearMonth month) { return YearMonth.parse(expiry).isBefore(month); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getNickname() { return nickname; }
    public String getNetwork() { return network; }
    public String getLastFour() { return lastFour; }
    public String getExpiry() { return expiry; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getSingleLimit() { return singleLimit; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public boolean isOverseasEnabled() { return overseasEnabled; }
    public boolean isFrozen() { return frozen; }
    public String getCurrency() { return currency; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setSingleLimit(BigDecimal singleLimit) { this.singleLimit = singleLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public void setOverseasEnabled(boolean overseasEnabled) { this.overseasEnabled = overseasEnabled; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
}

