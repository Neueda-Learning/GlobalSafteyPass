package com.globalsafetypass.api;

import com.globalsafetypass.repository.*;
import com.globalsafetypass.service.ReadinessService;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final UserRepository users;
    private final TripRepository trips;
    private final TransactionRepository transactions;
    private final RiskAlertRepository alerts;
    private final ReadinessService readiness;

    public DashboardController(UserRepository users, TripRepository trips,
                               TransactionRepository transactions, RiskAlertRepository alerts,
                               ReadinessService readiness) {
        this.users = users; this.trips = trips; this.transactions = transactions;
        this.alerts = alerts; this.readiness = readiness;
    }

    @GetMapping
    public Map<String, Object> get() {
        var user = users.findById(1L).orElseThrow();
        var trip = trips.findByUserIdOrderByStartDateDesc(1L).stream().findFirst().orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("trip", trip);
        result.put("readiness", trip == null ? null : readiness.evaluate(trip, false));
        result.put("recentTransactions", transactions.findTop50ByOrderByOccurredAtDesc()
            .stream().limit(4).toList());
        result.put("openAlertCount", alerts.countByStatus(
            com.globalsafetypass.model.RiskAlert.Status.OPEN));
        return result;
    }
}

