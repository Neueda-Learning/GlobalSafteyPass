package travelassistant.service;

import travelassistant.model.Card;
import travelassistant.model.Transaction;
import travelassistant.model.TransactionStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class FailureCodeService {

    private static final Map<String, String> FAILURE_MESSAGES = new HashMap<>();
    private static final Map<String, String> RECOMMENDED_ACTIONS = new HashMap<>();

    static {
        FAILURE_MESSAGES.put("INSUFFICIENT_BALANCE", "Insufficient balance to complete this payment");
        FAILURE_MESSAGES.put("CARD_FROZEN", "Card is frozen and cannot be used for transactions");
        FAILURE_MESSAGES.put("LIMIT_EXCEEDED", "Transaction amount exceeds the daily spending limit");
        FAILURE_MESSAGES.put("OVERSEAS_DISABLED", "Overseas payments are not enabled");
        FAILURE_MESSAGES.put("CARD_EXPIRED", "Card has expired");
        FAILURE_MESSAGES.put("DUPLICATE_CHARGE", "Duplicate charge detected");

        RECOMMENDED_ACTIONS.put("INSUFFICIENT_BALANCE", "Top up and retry, or use another payment method");
        RECOMMENDED_ACTIONS.put("CARD_FROZEN", "Unfreeze the card and retry, or contact support");
        RECOMMENDED_ACTIONS.put("LIMIT_EXCEEDED", "Temporarily increase the spending limit and retry");
        RECOMMENDED_ACTIONS.put("OVERSEAS_DISABLED", "Enable overseas payments and retry");
        RECOMMENDED_ACTIONS.put("CARD_EXPIRED", "Use a card with a valid expiry date");
        RECOMMENDED_ACTIONS.put("DUPLICATE_CHARGE", "If confirmed as duplicate, submit a dispute");
    }

    public String getMessage(String code) {
        return FAILURE_MESSAGES.getOrDefault(code, "Payment failed, please try again later");
    }

    public String getRecommendedAction(String code) {
        return RECOMMENDED_ACTIONS.getOrDefault(code, "Please contact customer support for assistance");
    }

    public String detectFailure(Card card, BigDecimal amount, String simulateFailure) {
        if (simulateFailure != null && !simulateFailure.isBlank()) {
            return simulateFailure;
        }
        if (card.isFrozen()) return "CARD_FROZEN";
        if (card.getExpiryDate().isBefore(java.time.LocalDate.now())) return "CARD_EXPIRED";
        if (!card.isOverseasEnabled()) return "OVERSEAS_DISABLED";
        if (amount.compareTo(card.getDailyLimit()) > 0) return "LIMIT_EXCEEDED";
        if (amount.compareTo(card.getBalance()) > 0) return "INSUFFICIENT_BALANCE";
        return null;
    }

    public void applyFailure(Transaction transaction, String code) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureCode(code);
        transaction.setFailureMessage(getMessage(code) + " Recommendation: " + getRecommendedAction(code));
    }
}
