package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.integration.ExchangeRateProvider;
import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("JourneyExchangeRateService")
class JourneyExchangeRateServiceTest {
    private TripService trips;
    private CardRepository cards;
    private AccountRepository accounts;
    private CardFxRateRepository storedRates;
    private JourneyExchangeRateService service;

    @BeforeEach
    void setUp() {
        trips = mock(TripService.class);
        cards = mock(CardRepository.class);
        accounts = mock(AccountRepository.class);
        storedRates = mock(CardFxRateRepository.class);
        ExchangeRateProvider provider = (s, t, d) -> new ExchangeRateQuote(s, t, new BigDecimal("149.25"), d, "Test", false);
        ExchangeRateService rates = new ExchangeRateService(provider);
        service = new JourneyExchangeRateService(trips, cards, accounts, rates, storedRates, new CardCapabilityService());
    }

    @Test @DisplayName("returns rate for Japan trip")
    void returnsRateForJapanTrip() {
        Trip trip = TestFixtures.trip("trip-1", "customer-001");
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        Account account = TestFixtures.activeAccount("acct-001", "customer-001");
        when(trips.owned("customer-001", "trip-1")).thenReturn(trip);
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        when(accounts.findById("acct-001")).thenReturn(Optional.of(account));
        when(storedRates.findByCardIdAndTargetCurrency(anyString(), anyString())).thenReturn(Optional.empty());
        when(storedRates.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = service.get("customer-001", "trip-1");
        assertThat(response.destinationCurrency()).isEqualTo("JPY");
        assertThat(response.cardSupportsCurrency()).isTrue();
        assertThat(response.rate()).isNotNull();
    }

    @Test @DisplayName("unknown destination returns guidance")
    void unknownDestinationReturnsGuidance() {
        Trip trip = TestFixtures.trip("trip-1", "customer-001");
        trip.setDestinationCountry("Antarctica");
        Card card = TestFixtures.activeCard("card-001", "customer-001");
        Account account = TestFixtures.activeAccount("acct-001", "customer-001");
        when(trips.owned("customer-001", "trip-1")).thenReturn(trip);
        when(cards.findById("card-001")).thenReturn(Optional.of(card));
        when(accounts.findById("acct-001")).thenReturn(Optional.of(account));
        var response = service.get("customer-001", "trip-1");
        assertThat(response.destinationCurrencyVerified()).isFalse();
        assertThat(response.recommendation()).contains("could not be verified");
    }
}
