package travelassistant.service;

import travelassistant.dto.ReadinessResult;
import travelassistant.dto.TripRequest;
import travelassistant.model.Card;
import travelassistant.model.Trip;
import travelassistant.repository.CardRepository;
import travelassistant.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final CardRepository cardRepository;
    private final ReadinessService readinessService;

    public TripService(TripRepository tripRepository, CardRepository cardRepository, ReadinessService readinessService) {
        this.tripRepository = tripRepository;
        this.cardRepository = cardRepository;
        this.readinessService = readinessService;
    }

    @Transactional
    public Trip createTrip(TripRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Return date cannot be before departure date");
        }

        Card card = cardRepository.findById(request.getPreferredCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        Trip trip = new Trip();
        trip.setDestination(request.getDestination());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setBudget(request.getBudget());
        trip.setCurrency(request.getCurrency());
        trip.setPreferredCard(card);

        ReadinessResult readiness = readinessService.evaluate(trip, card);
        trip.setReadinessScore(readiness.getScore());
        trip.setStatus(readiness.getScore() >= 80 ? "READY" : "NEEDS_ATTENTION");

        return tripRepository.save(trip);
    }

    public ReadinessResult checkReadiness(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
        return readinessService.evaluate(trip, trip.getPreferredCard());
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
    }
}
