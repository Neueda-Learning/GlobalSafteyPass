package travelassistant.controller;

import travelassistant.dto.PaymentRequest;
import travelassistant.dto.TripRequest;
import travelassistant.model.*;
import travelassistant.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final TripService tripService;
    private final CardService cardService;
    private final PaymentService paymentService;
    private final MonitoringService monitoringService;

    public ApiController(TripService tripService, CardService cardService,
                         PaymentService paymentService, MonitoringService monitoringService) {
        this.tripService = tripService;
        this.cardService = cardService;
        this.paymentService = paymentService;
        this.monitoringService = monitoringService;
    }

    @GetMapping("/cards")
    public List<Card> getCards() {
        return cardService.getAllCards();
    }

    @GetMapping("/trips")
    public List<Trip> getTrips() {
        return tripService.getAllTrips();
    }

    @GetMapping("/trips/{id}")
    public Trip getTrip(@PathVariable Long id) {
        return tripService.getTrip(id);
    }

    @PostMapping("/trips")
    public Trip createTrip(@Valid @RequestBody TripRequest request) {
        return tripService.createTrip(request);
    }

    @GetMapping("/trips/{id}/readiness")
    public Object getReadiness(@PathVariable Long id) {
        return tripService.checkReadiness(id);
    }

    @PostMapping("/payments")
    public Transaction processPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @GetMapping("/trips/{id}/transactions")
    public List<Transaction> getTransactions(@PathVariable Long id) {
        return paymentService.getTransactionsByTrip(id);
    }

    @GetMapping("/trips/{id}/dashboard")
    public Object getDashboard(@PathVariable Long id) {
        return paymentService.getDashboard(id);
    }

    @GetMapping("/alerts")
    public List<Alert> getAlerts() {
        return monitoringService.getAllAlerts();
    }

    @PostMapping("/alerts/{id}/confirm")
    public Alert confirmAlert(@PathVariable Long id) {
        return monitoringService.confirmAlert(id);
    }

    @PostMapping("/alerts/{id}/report")
    public Alert reportAlert(@PathVariable Long id) {
        return monitoringService.reportAlert(id);
    }

    @PostMapping("/alerts/{id}/dismiss")
    public Alert dismissAlert(@PathVariable Long id) {
        return monitoringService.dismissAlert(id);
    }

    @PostMapping("/cards/{id}/enable-overseas")
    public Card enableOverseas(@PathVariable Long id) {
        return cardService.enableOverseas(id);
    }

    @PostMapping("/cards/{id}/increase-limit")
    public Card increaseLimit(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        return cardService.increaseLimit(id, body.get("limit"));
    }

    @PostMapping("/cards/{id}/freeze")
    public Card freezeCard(@PathVariable Long id) {
        return cardService.freezeCard(id);
    }

    @PostMapping("/cards/{id}/unfreeze")
    public Card unfreezeCard(@PathVariable Long id) {
        return cardService.unfreezeCard(id);
    }
}
