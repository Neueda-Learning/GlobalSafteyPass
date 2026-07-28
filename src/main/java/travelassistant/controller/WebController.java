package travelassistant.controller;

import travelassistant.dto.ReadinessResult;
import travelassistant.dto.TripDashboard;
import travelassistant.dto.TripRequest;
import travelassistant.model.*;
import travelassistant.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class WebController {

    private final TripService tripService;
    private final CardService cardService;
    private final PaymentService paymentService;
    private final MonitoringService monitoringService;

    public WebController(TripService tripService, CardService cardService,
                         PaymentService paymentService, MonitoringService monitoringService) {
        this.tripService = tripService;
        this.cardService = cardService;
        this.paymentService = paymentService;
        this.monitoringService = monitoringService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("trips", tripService.getAllTrips());
        model.addAttribute("pendingAlerts", monitoringService.getPendingAlerts().size());
        return "index";
    }

    @GetMapping("/trips/new")
    public String newTrip(Model model) {
        model.addAttribute("tripRequest", new TripRequest());
        model.addAttribute("cards", cardService.getAllCards());
        return "trip-setup";
    }

    @PostMapping("/trips")
    public String createTrip(@Valid @ModelAttribute TripRequest tripRequest,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("cards", cardService.getAllCards());
            return "trip-setup";
        }
        try {
            Trip trip = tripService.createTrip(tripRequest);
            redirect.addFlashAttribute("success", "Trip created successfully!");
            return "redirect:/trips/" + trip.getId() + "/readiness";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cards", cardService.getAllCards());
            return "trip-setup";
        }
    }

    @GetMapping("/trips/{id}/readiness")
    public String readiness(@PathVariable Long id, Model model) {
        Trip trip = tripService.getTrip(id);
        ReadinessResult readiness = tripService.checkReadiness(id);
        model.addAttribute("trip", trip);
        model.addAttribute("readiness", readiness);
        model.addAttribute("card", trip.getPreferredCard());
        return "readiness";
    }

    @GetMapping("/trips/{id}/dashboard")
    public String dashboard(@PathVariable Long id, Model model) {
        Trip trip = tripService.getTrip(id);
        TripDashboard dashboard = paymentService.getDashboard(id);
        List<Transaction> transactions = paymentService.getTransactionsByTrip(id);
        model.addAttribute("trip", trip);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("transactions", transactions);
        model.addAttribute("cards", cardService.getAllCards());
        model.addAttribute("categories", SpendingCategory.values());
        return "dashboard";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("alerts", monitoringService.getAllAlerts());
        return "alerts";
    }

    @PostMapping("/alerts/{id}/confirm")
    public String confirmAlert(@PathVariable Long id) {
        monitoringService.confirmAlert(id);
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/{id}/report")
    public String reportAlert(@PathVariable Long id) {
        monitoringService.reportAlert(id);
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/{id}/dismiss")
    public String dismissAlert(@PathVariable Long id) {
        monitoringService.dismissAlert(id);
        return "redirect:/alerts";
    }

    @PostMapping("/cards/{id}/enable-overseas")
    public String enableOverseas(@PathVariable Long id, @RequestParam Long tripId) {
        cardService.enableOverseas(id);
        return "redirect:/trips/" + tripId + "/readiness";
    }

    @PostMapping("/cards/{id}/unfreeze")
    public String unfreezeCard(@PathVariable Long id, @RequestParam Long tripId) {
        cardService.unfreezeCard(id);
        return "redirect:/trips/" + tripId + "/readiness";
    }

    @PostMapping("/cards/{id}/freeze")
    public String freezeCard(@PathVariable Long id) {
        cardService.freezeCard(id);
        return "redirect:/alerts";
    }
}
