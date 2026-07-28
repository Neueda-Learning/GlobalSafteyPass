package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class JourneyExchangeRateService {
    private static final Map<String,String> DESTINATION_CURRENCY=Map.of(
            "Japan","JPY","France","EUR","Singapore","SGD","China","CNY","Canada","CAD",
            "United Kingdom","GBP","United States","USD");
    private final TripService trips;private final CardRepository cards;private final AccountRepository accounts;private final ExchangeRateService rates;private final CardFxRateRepository storedRates;private final CardCapabilityService capabilities;
    public JourneyExchangeRateService(TripService trips,CardRepository cards,AccountRepository accounts,ExchangeRateService rates,CardFxRateRepository storedRates,CardCapabilityService capabilities){
        this.trips=trips;this.cards=cards;this.accounts=accounts;this.rates=rates;this.storedRates=storedRates;this.capabilities=capabilities;}
    public JourneyExchangeRateResponse get(String customer,String tripId){
        Trip trip=trips.owned(customer,tripId);Card card=cards.findById(trip.getPreferredCardId()).orElseThrow();
        Account account=accounts.findById(card.getLinkedAccountId()).orElseThrow();
        boolean verified=DESTINATION_CURRENCY.containsKey(trip.getDestinationCountry());
        String destinationCurrency=DESTINATION_CURRENCY.get(trip.getDestinationCountry());
        String mainCurrency=card.getMainCurrency()==null?account.getCurrency():card.getMainCurrency();
        boolean supported=verified&&capabilities.supportsCurrency(card,destinationCurrency);
        if(!verified)return new JourneyExchangeRateResponse(tripId,trip.getDestinationCountry(),card.getId(),card.getMaskedCardNumber(),
                mainCurrency,null,null,null,null,true,Instant.now(),false,false,
                "Destination currency could not be verified. Choose another card in Trip Details or plan to exchange cash on arrival.");
        capabilities.supportedCurrencies(card).stream()
                .filter(currency->!currency.isBlank()&&!currency.equals(mainCurrency))
                .forEach(currency->storeRate(card.getId(),mainCurrency,currency));
        ExchangeRateQuote quote=rates.rate(mainCurrency,destinationCurrency,LocalDate.now());
        persist(card.getId(),mainCurrency,destinationCurrency,quote);
        String recommendation=supported?"This preferred card supports the destination currency."
                :"This card does not list the destination currency. Switch cards in Trip Details or exchange cash after arrival.";
        return new JourneyExchangeRateResponse(tripId,trip.getDestinationCountry(),card.getId(),card.getMaskedCardNumber(),
                mainCurrency,destinationCurrency,quote.rate(),quote.rateDate(),quote.provider(),quote.estimated(),Instant.now(),true,supported,recommendation);
    }
    private void storeRate(String cardId,String source,String target){
        ExchangeRateQuote quote=rates.rate(source,target,LocalDate.now());
        persist(cardId,source,target,quote);
    }
    private void persist(String cardId,String source,String target,ExchangeRateQuote quote){
        CardFxRate stored=storedRates.findByCardIdAndTargetCurrency(cardId,target).orElseGet(()->CardFxRate.builder().id(UUID.randomUUID().toString()).cardId(cardId).targetCurrency(target).build());
        stored.setSourceCurrency(source);stored.setRate(quote.rate());stored.setProvider(quote.provider());stored.setEstimated(quote.estimated());stored.setUpdatedAt(Instant.now());storedRates.save(stored);
    }
}
