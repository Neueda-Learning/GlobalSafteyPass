package travelassistant.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotNull(message = "Card ID is required")
    private Long cardId;

    @NotBlank(message = "Merchant name is required")
    private String merchant;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private BigDecimal exchangeRate;

    @NotBlank(message = "Spending category is required")
    private String category;

    private String location;

    private String simulateFailure;

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSimulateFailure() { return simulateFailure; }
    public void setSimulateFailure(String simulateFailure) { this.simulateFailure = simulateFailure; }
}
