package com.globalsafetypass.api;

import com.globalsafetypass.api.ApiDtos.AlertAction;
import com.globalsafetypass.model.*;
import com.globalsafetypass.repository.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final RiskAlertRepository alerts;
    private final FraudReportRepository reports;
    private final CardRepository cards;

    public AlertController(RiskAlertRepository alerts, FraudReportRepository reports,
                           CardRepository cards) {
        this.alerts = alerts; this.reports = reports; this.cards = cards;
    }

    @GetMapping
    public List<RiskAlert> list() { return alerts.findAllByOrderByCreatedAtDesc(); }

    @PostMapping("/{id}/actions")
    public RiskAlert act(@PathVariable Long id, @Valid @RequestBody AlertAction input) {
        RiskAlert alert = alerts.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found."));
        switch (input.action().toUpperCase()) {
            case "CONFIRM" -> alert.setStatus(RiskAlert.Status.CONFIRMED_SAFE);
            case "DISMISS" -> alert.setStatus(RiskAlert.Status.DISMISSED);
            case "REPORT" -> {
                reports.save(new FraudReport(alert,
                    input.reason() == null ? "Transaction not recognised" : input.reason(),
                    input.notes()));
                alert.setStatus(RiskAlert.Status.REPORTED);
            }
            case "FREEZE" -> {
                Card card = alert.getTransaction().getCard();
                card.setFrozen(true);
                cards.save(card);
                alert.setStatus(RiskAlert.Status.CARD_FROZEN);
            }
            default -> throw new IllegalArgumentException("Unsupported alert action.");
        }
        return alerts.save(alert);
    }
}

