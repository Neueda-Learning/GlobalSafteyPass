package com.globalsafetypass.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "external_transaction_id"}),
    @UniqueConstraint(columnNames = {"event_id"})
})
public class BankTransaction {
    public enum Status { APPROVED, DECLINED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Card card;
    @ManyToOne private Trip trip;
    @Column(nullable = false) private String provider;
    @Column(name = "external_transaction_id", nullable = false) private String externalTransactionId;
    @Column(name = "event_id", nullable = false) private String eventId;
    @Column(nullable = false) private String merchant;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, precision = 14, scale = 6) private BigDecimal exchangeRate;
    @Column(nullable = false) private String country;
    @Column(nullable = false) private String city;
    @Column(nullable = false) private String category;
    @Column(nullable = false) private String channel;
    @Column(nullable = false) private LocalDateTime occurredAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    private String declineCode;
    private String declineMessage;
    private String recommendedAction;

    public BankTransaction() {}
    public Long getId() { return id; }
    public Card getCard() { return card; }
    public Trip getTrip() { return trip; }
    public String getProvider() { return provider; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public String getEventId() { return eventId; }
    public String getMerchant() { return merchant; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public String getCategory() { return category; }
    public String getChannel() { return channel; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public Status getStatus() { return status; }
    public String getDeclineCode() { return declineCode; }
    public String getDeclineMessage() { return declineMessage; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setCard(Card value) { card = value; }
    public void setTrip(Trip value) { trip = value; }
    public void setProvider(String value) { provider = value; }
    public void setExternalTransactionId(String value) { externalTransactionId = value; }
    public void setEventId(String value) { eventId = value; }
    public void setMerchant(String value) { merchant = value; }
    public void setAmount(BigDecimal value) { amount = value; }
    public void setCurrency(String value) { currency = value; }
    public void setExchangeRate(BigDecimal value) { exchangeRate = value; }
    public void setCountry(String value) { country = value; }
    public void setCity(String value) { city = value; }
    public void setCategory(String value) { category = value; }
    public void setChannel(String value) { channel = value; }
    public void setOccurredAt(LocalDateTime value) { occurredAt = value; }
    public void setStatus(Status value) { status = value; }
    public void setDeclineCode(String value) { declineCode = value; }
    public void setDeclineMessage(String value) { declineMessage = value; }
    public void setRecommendedAction(String value) { recommendedAction = value; }
}

