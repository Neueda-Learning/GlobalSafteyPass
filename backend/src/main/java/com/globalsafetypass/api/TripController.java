package com.globalsafetypass.api;

import com.globalsafetypass.api.ApiDtos.*;
import com.globalsafetypass.model.*;
import com.globalsafetypass.repository.*;
import com.globalsafetypass.service.ReadinessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripRepository trips;
    private final UserRepository users;
    private final CardRepository cards;
    private final ReadinessService readiness;

    public TripController(TripRepository trips, UserRepository users, CardRepository cards,
                          ReadinessService readiness) {
        this.trips = trips; this.users = users; this.cards = cards; this.readiness = readiness;
    }

    @GetMapping
    public List<Trip> list() { return trips.findByUserIdOrderByStartDateDesc(1L); }

    @PostMapping
    public Trip create(@Valid @RequestBody TripRequest request) {
        if (!request.endDate().isAfter(request.startDate()))
            throw new IllegalArgumentException("Return date must be after the departure date.");
        User user = users.findById(1L).orElseThrow();
        Card card = cards.findById(request.cardId())
            .filter(c -> c.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Card not found."));
        Trip trip = trips.save(new Trip(user, card, request.destinationCountry(),
            request.destinationCity(), request.startDate(), request.endDate(),
            request.budget(), request.currency().toUpperCase()));
        readiness.evaluate(trip, true);
        return trip;
    }

    @GetMapping("/{id}/readiness")
    public ReadinessResult readiness(@PathVariable Long id) {
        return readiness.evaluate(trips.findById(id).orElseThrow(), false);
    }

    @PostMapping("/{id}/readiness")
    public ReadinessResult runReadiness(@PathVariable Long id) {
        return readiness.evaluate(trips.findById(id).orElseThrow(), true);
    }
}
