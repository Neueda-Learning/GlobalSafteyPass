package travelassistant.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TripRequest {

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotNull(message = "Departure date is required")
    private LocalDate startDate;

    @NotNull(message = "Return date is required")
    private LocalDate endDate;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal budget;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Please select a preferred card")
    private Long preferredCardId;

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getPreferredCardId() { return preferredCardId; }
    public void setPreferredCardId(Long preferredCardId) { this.preferredCardId = preferredCardId; }
}
