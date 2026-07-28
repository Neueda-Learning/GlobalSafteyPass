package travelassistant.service;

import travelassistant.dto.ReadinessResult;
import travelassistant.model.Card;
import travelassistant.model.Trip;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReadinessService {

    public static final String ACTION_ENABLE_OVERSEAS = "ENABLE_OVERSEAS";
    public static final String ACTION_UNFREEZE_CARD = "UNFREEZE_CARD";

    public ReadinessResult evaluate(Trip trip, Card card) {
        ReadinessResult result = new ReadinessResult();
        List<ReadinessResult.WarningItem> warnings = new ArrayList<>();
        List<ReadinessResult.ActionItem> actions = new ArrayList<>();
        int score = 100;

        if (trip.getEndDate().isBefore(trip.getStartDate())) {
            warnings.add(new ReadinessResult.WarningItem("DATE_INVALID", "Return date cannot be before departure date"));
            actions.add(new ReadinessResult.ActionItem("FIX_DATES", "Please select a valid travel date range"));
            score -= 40;
        }

        if (trip.getStartDate().isBefore(LocalDate.now())) {
            warnings.add(new ReadinessResult.WarningItem("DATE_PAST", "Departure date has already passed"));
            actions.add(new ReadinessResult.ActionItem("UPDATE_DATES", "Please set the departure date to today or later"));
            score -= 15;
        }

        if (card.getExpiryDate().isBefore(trip.getEndDate())) {
            warnings.add(new ReadinessResult.WarningItem("CARD_EXPIRED",
                    "Card will expire before the trip ends (expiry: " + card.getExpiryDate() + ")"));
            actions.add(new ReadinessResult.ActionItem("CHANGE_CARD", "Please choose a card valid for the entire trip"));
            score -= 30;
        }

        if (!card.isOverseasEnabled()) {
            warnings.add(new ReadinessResult.WarningItem("OVERSEAS_DISABLED", "Overseas payments are not enabled"));
            actions.add(new ReadinessResult.ActionItem(ACTION_ENABLE_OVERSEAS, "Enable overseas payments in card settings"));
            score -= 25;
        }

        if (card.isFrozen()) {
            warnings.add(new ReadinessResult.WarningItem("CARD_FROZEN", "Card is currently frozen"));
            actions.add(new ReadinessResult.ActionItem(ACTION_UNFREEZE_CARD, "Contact support or unfreeze the card online"));
            score -= 35;
        }

        if (card.getBalance().compareTo(trip.getBudget()) < 0) {
            warnings.add(new ReadinessResult.WarningItem("LOW_BALANCE",
                    "Card balance (" + card.getBalance() + ") is below trip budget (" + trip.getBudget() + ")"));
            actions.add(new ReadinessResult.ActionItem("TOP_UP", "Top up the card or adjust the trip budget"));
            score -= 20;
        }

        if (card.getDailyLimit().compareTo(BigDecimal.valueOf(500)) < 0) {
            warnings.add(new ReadinessResult.WarningItem("LOW_LIMIT",
                    "Daily spending limit is low (" + card.getDailyLimit() + ")"));
            actions.add(new ReadinessResult.ActionItem("INCREASE_LIMIT", "Consider temporarily increasing the daily limit"));
            score -= 10;
        }

        score = Math.max(0, score);
        result.setScore(score);
        result.setLevel(getLevel(score));
        result.setWarnings(warnings);
        result.setActions(actions);
        return result;
    }

    private String getLevel(int score) {
        if (score >= 80) return "READY";
        if (score >= 50) return "CAUTION";
        return "NOT_READY";
    }
}
