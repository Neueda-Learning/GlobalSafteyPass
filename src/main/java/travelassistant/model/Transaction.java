package travelassistant.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private String merchant;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpendingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    private String failureCode;

    private String failureMessage;

    @Column(nullable = false)
    private LocalDateTime transactionTime = LocalDateTime.now();

    private String location;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public Card getCard() { return card; }
    public void setCard(Card card) { this.card = card; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
    public SpendingCategory getCategory() { return category; }
    public void setCategory(SpendingCategory category) { this.category = category; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public LocalDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
