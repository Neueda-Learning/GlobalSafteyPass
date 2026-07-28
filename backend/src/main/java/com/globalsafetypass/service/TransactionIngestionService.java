package com.globalsafetypass.service;

import com.globalsafetypass.api.ApiDtos.*;
import com.globalsafetypass.model.*;
import com.globalsafetypass.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class TransactionIngestionService {
    private final CardRepository cards;
    private final TripRepository trips;
    private final TransactionRepository transactions;
    private final RiskService riskService;

    public TransactionIngestionService(CardRepository cards, TripRepository trips,
                                       TransactionRepository transactions, RiskService riskService) {
        this.cards = cards; this.trips = trips; this.transactions = transactions;
        this.riskService = riskService;
    }

    @Transactional
    public IngestionResult ingest(BankEvent event) {
        if (transactions.existsByEventId(event.eventId())
            || transactions.existsByProviderAndExternalTransactionId(
                event.provider(), event.externalTransactionId())) {
            return new IngestionResult(true, null, "IGNORED",
                "This bank event has already been processed.", null);
        }

        Card card = cards.findById(event.cardId())
            .orElseThrow(() -> new IllegalArgumentException("Card not found."));
        List<Trip> userTrips = trips.findByUserIdOrderByStartDateDesc(card.getUser().getId());
        Trip matched = matchTrip(userTrips, event);
        Trip nearest = userTrips.stream().min(Comparator.comparingLong(t ->
            Math.abs(ChronoUnit.DAYS.between(t.getStartDate(), event.occurredAt().toLocalDate()))))
            .orElse(null);

        BankTransaction tx = new BankTransaction();
        tx.setCard(card); tx.setTrip(matched); tx.setProvider(event.provider());
        tx.setExternalTransactionId(event.externalTransactionId()); tx.setEventId(event.eventId());
        tx.setMerchant(event.merchant()); tx.setAmount(event.amount());
        tx.setCurrency(event.currency().toUpperCase()); tx.setExchangeRate(event.exchangeRate());
        tx.setCountry(event.country()); tx.setCity(event.city()); tx.setCategory(event.category());
        tx.setChannel(event.channel()); tx.setOccurredAt(event.occurredAt());

        BigDecimal billingAmount = event.amount().multiply(event.exchangeRate());
        Decline decline = decide(card, event, billingAmount);
        if (decline == null) {
            tx.setStatus(BankTransaction.Status.APPROVED);
            card.setBalance(card.getBalance().subtract(billingAmount));
            cards.save(card);
            if (matched != null) {
                matched.addSpend(billingAmount);
                trips.save(matched);
            }
        } else {
            tx.setStatus(BankTransaction.Status.DECLINED);
            tx.setDeclineCode(decline.code());
            tx.setDeclineMessage(decline.message());
            tx.setRecommendedAction(decline.action());
        }

        tx = transactions.save(tx);
        if (tx.getStatus() == BankTransaction.Status.APPROVED) {
            riskService.evaluate(tx, nearest);
        }
        return new IngestionResult(false, tx.getId(), tx.getStatus().name(),
            tx.getStatus() == BankTransaction.Status.APPROVED
                ? (matched == null ? "Payment approved and marked as unmatched."
                                   : "Payment approved and added to your trip.")
                : tx.getDeclineMessage(),
            tx.getRecommendedAction());
    }

    private Trip matchTrip(List<Trip> candidates, BankEvent event) {
        LocalDate date = event.occurredAt().toLocalDate();
        return candidates.stream()
            .filter(t -> !date.isBefore(t.getStartDate()) && !date.isAfter(t.getEndDate()))
            .filter(t -> event.country().equalsIgnoreCase(t.getDestinationCountry())
                || event.city().equalsIgnoreCase(t.getDestinationCity()))
            .findFirst().orElse(null);
    }

    private Decline decide(Card card, BankEvent event, BigDecimal amount) {
        if (card.isFrozen()) return new Decline("CARD_FROZEN", "Your card is currently frozen.",
            "Unfreeze the card before trying again.");
        if (!card.isOverseasEnabled()) return new Decline("OVERSEAS_DISABLED",
            "Overseas payments are disabled.", "Enable overseas payments.");
        if (card.isExpiredAt(YearMonth.from(event.occurredAt())))
            return new Decline("CARD_EXPIRED", "This card has expired.", "Select another card.");
        if (amount.compareTo(card.getBalance()) > 0)
            return new Decline("INSUFFICIENT_FUNDS", "Your available balance is too low.",
                "Use another card or add funds.");
        if (amount.compareTo(card.getSingleLimit()) > 0)
            return new Decline("SINGLE_LIMIT_EXCEEDED",
                "This payment exceeds your transaction limit.",
                "Increase the limit or split the payment.");
        BigDecimal todayTotal = transactions.findByCardIdAndStatusAndOccurredAtBetween(
                card.getId(), BankTransaction.Status.APPROVED,
                event.occurredAt().toLocalDate().atStartOfDay(),
                event.occurredAt().toLocalDate().atTime(LocalTime.MAX))
            .stream().map(t -> t.getAmount().multiply(t.getExchangeRate()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (todayTotal.add(amount).compareTo(card.getDailyLimit()) > 0)
            return new Decline("DAILY_LIMIT_EXCEEDED",
                "You have reached your daily spending limit.",
                "Adjust the daily limit or try again tomorrow.");
        return null;
    }

    private record Decline(String code, String message, String action) {}
}
