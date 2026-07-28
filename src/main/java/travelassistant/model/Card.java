package travelassistant.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cardName;

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private boolean overseasEnabled = true;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyLimit = BigDecimal.valueOf(10000);

    @Column(nullable = false)
    private boolean frozen = false;

    @Column(nullable = false)
    private String currency = "CNY";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public boolean isOverseasEnabled() { return overseasEnabled; }
    public void setOverseasEnabled(boolean overseasEnabled) { this.overseasEnabled = overseasEnabled; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMaskedNumber() {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
