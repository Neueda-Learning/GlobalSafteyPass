package travelassistant.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TripDashboard {

    private Long tripId;
    private String destination;
    private BigDecimal budget;
    private BigDecimal totalSpent;
    private BigDecimal remaining;
    private String currency;
    private int transactionCount;
    private Map<String, BigDecimal> categoryBreakdown = new HashMap<>();

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    public BigDecimal getRemaining() { return remaining; }
    public void setRemaining(BigDecimal remaining) { this.remaining = remaining; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
    public Map<String, BigDecimal> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(Map<String, BigDecimal> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
}
