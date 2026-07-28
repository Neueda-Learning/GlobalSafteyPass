package travelassistant.service;

import travelassistant.dto.PaymentRequest;
import travelassistant.dto.TripDashboard;
import travelassistant.model.*;
import travelassistant.repository.CardRepository;
import travelassistant.repository.TransactionRepository;
import travelassistant.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final TripRepository tripRepository;
    private final CardRepository cardRepository;
    private final FailureCodeService failureCodeService;
    private final MonitoringService monitoringService;

    public PaymentService(TransactionRepository transactionRepository,
                          TripRepository tripRepository,
                          CardRepository cardRepository,
                          FailureCodeService failureCodeService,
                          MonitoringService monitoringService) {
        this.transactionRepository = transactionRepository;
        this.tripRepository = tripRepository;
        this.cardRepository = cardRepository;
        this.failureCodeService = failureCodeService;
        this.monitoringService = monitoringService;
    }

    @Transactional
    public Transaction processPayment(PaymentRequest request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        Transaction transaction = new Transaction();
        transaction.setTrip(trip);
        transaction.setCard(card);
        transaction.setMerchant(request.getMerchant());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setExchangeRate(request.getExchangeRate() != null ? request.getExchangeRate() : BigDecimal.ONE);
        transaction.setCategory(SpendingCategory.valueOf(request.getCategory()));
        transaction.setLocation(request.getLocation());

        String failureCode = failureCodeService.detectFailure(card, request.getAmount(), request.getSimulateFailure());
        if (failureCode != null) {
            failureCodeService.applyFailure(transaction, failureCode);
        } else {
            transaction.setStatus(TransactionStatus.SUCCESS);
            card.setBalance(card.getBalance().subtract(request.getAmount()));
            cardRepository.save(card);
            trip.setStatus("ACTIVE");
            tripRepository.save(trip);
        }

        Transaction saved = transactionRepository.save(transaction);
        monitoringService.analyzeTransaction(saved);
        return saved;
    }

    public List<Transaction> getTransactionsByTrip(Long tripId) {
        return transactionRepository.findByTripIdOrderByTransactionTimeDesc(tripId);
    }

    public TripDashboard getDashboard(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        List<Transaction> successTxns = transactionRepository.findByTripIdAndStatus(tripId, TransactionStatus.SUCCESS);
        BigDecimal totalSpent = successTxns.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> breakdown = new HashMap<>();
        for (Transaction t : successTxns) {
            String cat = t.getCategory().name();
            breakdown.merge(cat, t.getAmount(), BigDecimal::add);
        }

        TripDashboard dashboard = new TripDashboard();
        dashboard.setTripId(tripId);
        dashboard.setDestination(trip.getDestination());
        dashboard.setBudget(trip.getBudget());
        dashboard.setTotalSpent(totalSpent);
        dashboard.setRemaining(trip.getBudget().subtract(totalSpent));
        dashboard.setCurrency(trip.getCurrency());
        dashboard.setTransactionCount(successTxns.size());
        dashboard.setCategoryBreakdown(breakdown);
        return dashboard;
    }
}
