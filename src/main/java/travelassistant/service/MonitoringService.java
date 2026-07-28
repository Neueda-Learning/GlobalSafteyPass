package travelassistant.service;

import travelassistant.model.*;
import travelassistant.repository.AlertRepository;
import travelassistant.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MonitoringService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = BigDecimal.valueOf(5000);

    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;

    public MonitoringService(AlertRepository alertRepository, TransactionRepository transactionRepository) {
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
    }

    public void analyzeTransaction(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.SUCCESS) return;

        Trip trip = transaction.getTrip();
        LocalDate txnDate = transaction.getTransactionTime().toLocalDate();

        if (txnDate.isBefore(trip.getStartDate()) || txnDate.isAfter(trip.getEndDate())) {
            createAlert(transaction, "OUTSIDE_TRIP_DATES", RiskLevel.HIGH,
                    "Transaction date (" + txnDate + ") is outside trip dates ("
                            + trip.getStartDate() + " ~ " + trip.getEndDate() + ")");
        }

        if (transaction.getLocation() != null && !transaction.getLocation().isBlank()
                && !transaction.getLocation().equalsIgnoreCase(trip.getDestination())
                && !trip.getDestination().contains(transaction.getLocation())
                && !transaction.getLocation().contains(trip.getDestination())) {
            createAlert(transaction, "LOCATION_MISMATCH", RiskLevel.HIGH,
                    "Transaction location (" + transaction.getLocation()
                            + ") does not match trip destination (" + trip.getDestination() + ")");
        }

        List<Transaction> duplicates = transactionRepository.findByMerchantAndAmountAndStatus(
                transaction.getMerchant(), transaction.getAmount(), TransactionStatus.SUCCESS);
        if (duplicates.size() > 1) {
            createAlert(transaction, "DUPLICATE_CHARGE", RiskLevel.MEDIUM,
                    "Duplicate transaction detected with merchant \"" + transaction.getMerchant()
                            + "\" for amount " + transaction.getAmount());
        }

        if (transaction.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            createAlert(transaction, "HIGH_VALUE", RiskLevel.MEDIUM,
                    "High-value transaction detected: " + transaction.getAmount()
                            + " " + transaction.getCurrency());
        }
    }

    private void createAlert(Transaction transaction, String type, RiskLevel risk, String reason) {
        Alert alert = new Alert();
        alert.setTransaction(transaction);
        alert.setTrip(transaction.getTrip());
        alert.setAlertType(type);
        alert.setRiskLevel(risk);
        alert.setReason(reason);
        alert.setStatus(AlertStatus.PENDING);
        alertRepository.save(alert);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Alert> getPendingAlerts() {
        return alertRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.PENDING);
    }

    public Alert confirmAlert(Long alertId) {
        return updateAlertStatus(alertId, AlertStatus.CONFIRMED);
    }

    public Alert reportAlert(Long alertId) {
        return updateAlertStatus(alertId, AlertStatus.REPORTED);
    }

    public Alert dismissAlert(Long alertId) {
        return updateAlertStatus(alertId, AlertStatus.DISMISSED);
    }

    private Alert updateAlertStatus(Long alertId, AlertStatus status) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setStatus(status);
        return alertRepository.save(alert);
    }
}
