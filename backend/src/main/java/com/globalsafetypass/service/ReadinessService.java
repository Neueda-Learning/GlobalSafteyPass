package com.globalsafetypass.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalsafetypass.api.ApiDtos.*;
import com.globalsafetypass.model.Card;
import com.globalsafetypass.model.ReadinessCheck;
import com.globalsafetypass.model.Trip;
import com.globalsafetypass.repository.ReadinessCheckRepository;
import org.springframework.stereotype.Service;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReadinessService {
    private final ReadinessCheckRepository checks;
    private final ObjectMapper mapper;

    public ReadinessService(ReadinessCheckRepository checks, ObjectMapper mapper) {
        this.checks = checks; this.mapper = mapper;
    }

    public ReadinessResult evaluate(Trip trip, boolean persist) {
        Card card = trip.getPreferredCard();
        List<ReadinessItem> items = new ArrayList<>();
        boolean dates = !trip.getEndDate().isBefore(trip.getStartDate());
        add(items, "dates", "Trip dates", dates, 20, "Your travel dates are valid.",
            "Update the trip dates.");
        boolean validCard = !card.isExpiredAt(YearMonth.from(trip.getEndDate()));
        add(items, "expiry", "Card validity", validCard, 20, "Your card stays valid for this trip.",
            "Choose a card that remains valid through your return date.");
        add(items, "overseas", "Overseas payments", card.isOverseasEnabled(), 20,
            "Overseas payments are enabled.", "Enable overseas payments.");
        add(items, "frozen", "Card status", !card.isFrozen(), 15,
            "Your card is ready to use.", "Unfreeze this card.");
        add(items, "balance", "Available balance", card.getBalance().compareTo(trip.getBudget()) >= 0, 15,
            "Your balance covers the trip budget.", "Add funds or lower the trip budget.");
        add(items, "limit", "Daily limit", card.getDailyLimit().compareTo(trip.getBudget().multiply(new java.math.BigDecimal("0.20"))) >= 0, 10,
            "Your daily limit looks suitable.", "Increase your daily payment limit.");
        int score = items.stream().filter(ReadinessItem::passed).mapToInt(ReadinessItem::points).sum();
        String status = score >= 80 ? "Ready" : score >= 60 ? "Needs attention" : "Action required";
        ReadinessResult result = new ReadinessResult(score, status, items);
        if (persist) {
            try {
                checks.save(new ReadinessCheck(trip, score, status, mapper.writeValueAsString(result)));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Could not save readiness result", e);
            }
        }
        return result;
    }

    private void add(List<ReadinessItem> items, String key, String label, boolean pass,
                     int points, String success, String action) {
        items.add(new ReadinessItem(key, label, pass, points,
            pass ? success : action, pass ? null : action));
    }
}

