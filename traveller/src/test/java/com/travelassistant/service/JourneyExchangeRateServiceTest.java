package com.travelassistant.service;

import com.travelassistant.exception.ExternalServiceException;
import com.travelassistant.model.Account;
import com.travelassistant.model.Card;
import com.travelassistant.model.CardFxRate;
import com.travelassistant.model.Enums;
import com.travelassistant.model.Trip;
import com.travelassistant.repository.AccountRepository;
import com.travelassistant.repository.CardFxRateRepository;
import com.travelassistant.repository.CardRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JourneyExchangeRateServiceTest {
    @Test void fallsBackToLastStoredLiveRateWhenProviderIsUnavailable(){
        TripService trips=mock(TripService.class);
        CardRepository cards=mock(CardRepository.class);
        AccountRepository accounts=mock(AccountRepository.class);
        ExchangeRateService rates=mock(ExchangeRateService.class);
        CardFxRateRepository storedRates=mock(CardFxRateRepository.class);
        CardCapabilityService capabilities=mock(CardCapabilityService.class);
        JourneyExchangeRateService service=new JourneyExchangeRateService(trips,cards,accounts,rates,storedRates,capabilities);

        Trip trip=Trip.builder().id("trip-1").customerId("customer-1").destinationCountry("Japan").destinationCity("Tokyo")
                .startDate(LocalDate.of(2026,8,10)).endDate(LocalDate.of(2026,8,18)).budget(new BigDecimal("2500"))
                .budgetCurrency("USD").preferredCardId("card-1").status(Enums.TripStatus.PLANNED).build();
        Card card=Card.builder().id("card-1").customerId("customer-1").linkedAccountId("account-1")
                .maskedCardNumber("**** 1234").mainCurrency("USD").supportedCurrencies("JPY,EUR").build();
        Account account=Account.builder().id("account-1").customerId("customer-1").currency("USD").status(Enums.AccountStatus.ACTIVE).build();
        CardFxRate stored=CardFxRate.builder().id("fx-1").cardId("card-1").sourceCurrency("USD").targetCurrency("JPY")
                .rate(new BigDecimal("149.25000000")).provider("Frankfurter").estimated(false)
                .updatedAt(Instant.parse("2026-07-27T08:15:00Z")).build();

        when(trips.owned("customer-1","trip-1")).thenReturn(trip);
        when(cards.findById("card-1")).thenReturn(Optional.of(card));
        when(accounts.findById("account-1")).thenReturn(Optional.of(account));
        when(capabilities.supportsCurrency(card,"JPY")).thenReturn(true);
        when(capabilities.supportedCurrencies(card)).thenReturn(List.of());
        when(rates.rate("USD","JPY",LocalDate.now())).thenThrow(new ExternalServiceException("offline",null));
        when(storedRates.findByCardIdAndTargetCurrency("card-1","JPY")).thenReturn(Optional.of(stored));

        var response=service.get("customer-1","trip-1");

        assertThat(response.rate()).isEqualByComparingTo(new BigDecimal("149.25000000"));
        assertThat(response.provider()).isEqualTo("Frankfurter-stored");
        assertThat(response.estimated()).isTrue();
        verify(storedRates).findByCardIdAndTargetCurrency("card-1","JPY");
    }
}
