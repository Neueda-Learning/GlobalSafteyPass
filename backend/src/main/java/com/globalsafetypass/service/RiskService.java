package com.globalsafetypass.service;

import com.globalsafetypass.model.*;
import com.globalsafetypass.repository.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RiskService {
    private final RiskAlertRepository alerts;
    private final TransactionRepository transactions;

    public RiskService(RiskAlertRepository alerts, TransactionRepository transactions) {
        this.alerts = alerts; this.transactions = transactions;
    }

    public void evaluate(BankTransaction tx, Trip nearestTrip) {
        Trip trip = tx.getTrip() != null ? tx.getTrip() : nearestTrip;
        if (trip == null) return;
        var date = tx.getOccurredAt().toLocalDate();
        if (date.isBefore(trip.getStartDate()) || date.isAfter(trip.getEndDate())) {
            create(tx, RiskAlert.Severity.HIGH, "OUTSIDE_TRIP_DATES",
                "Activity outside your trip", "This transaction occurred outside your planned travel dates.");
        }
        boolean destinationMatch = tx.getCountry().equalsIgnoreCase(trip.getDestinationCountry())
            || tx.getCity().equalsIgnoreCase(trip.getDestinationCity());
        if (!destinationMatch) {
            create(tx, RiskAlert.Severity.HIGH, "OUTSIDE_DESTINATION",
                "Unexpected location", "This transaction is outside your planned destination.");
        }
        List<BankTransaction> duplicates = transactions
            .findByCardIdAndMerchantIgnoreCaseAndAmountAndOccurredAtBetween(
                tx.getCard().getId(), tx.getMerchant(), tx.getAmount(),
                tx.getOccurredAt().minusMinutes(10), tx.getOccurredAt());
        if (duplicates.stream().anyMatch(other -> !other.getId().equals(tx.getId()))) {
            create(tx, RiskAlert.Severity.MEDIUM, "DUPLICATE_CHARGE",
                "Possible duplicate charge", "A matching charge appeared within ten minutes.");
        }
        BigDecimal converted = tx.getAmount().multiply(tx.getExchangeRate());
        if (converted.compareTo(trip.getBudget().multiply(new BigDecimal("0.30"))) > 0) {
            create(tx, RiskAlert.Severity.MEDIUM, "HIGH_VALUE",
                "Unusually large payment", "This payment is more than 30% of your trip budget.");
        }
        if ("ATM".equalsIgnoreCase(tx.getChannel())) {
            var withdrawals = transactions.findByCardIdAndChannelIgnoreCaseAndOccurredAtBetween(
                tx.getCard().getId(), "ATM", tx.getOccurredAt().minusHours(24), tx.getOccurredAt());
            if (converted.compareTo(new BigDecimal("500")) > 0 || withdrawals.size() >= 3) {
                create(tx, RiskAlert.Severity.HIGH, "UNUSUAL_WITHDRAWAL",
                    "Unusual cash withdrawal", "This ATM activity is larger or more frequent than expected.");
            }
        }
    }

    private void create(BankTransaction tx, RiskAlert.Severity severity,
                        String rule, String title, String reason) {
        alerts.save(new RiskAlert(tx, severity, rule, title, reason));
    }
}
