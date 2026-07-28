package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.model.Trip;
import com.travelassistant.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class CashExchangePlanningService {
    private static final Map<String,String> CURRENCY=Map.of(
            "Japan","JPY","France","EUR","Singapore","SGD","China","CNY",
            "Canada","CAD","United Kingdom","GBP","United States","USD");
    private static final Map<String,String> ATM=Map.of(
            "Japan","Airport, convenience-store and Japan Post ATMs are widely available.",
            "Canada","Bank and Interac ATMs are widely available.",
            "France","Bank ATMs are widely available in airports and city centres.",
            "Singapore","Bank ATMs are widely available at the airport and MRT stations.",
            "United Kingdom","Bank and LINK ATMs are widely available.");
    private final TripService tripService;private final TripRepository trips;
    private final ExchangeRateService rates;private final AuditService audit;
    public CashExchangePlanningService(TripService tripService,TripRepository trips,ExchangeRateService rates,AuditService audit){
        this.tripService=tripService;this.trips=trips;this.rates=rates;this.audit=audit;
    }
    @Transactional public CashExchangePlanResponse save(String customer,String id,CashExchangePlanRequest request){
        Trip trip=tripService.owned(customer,id);String currency=CURRENCY.get(trip.getDestinationCountry());
        BigDecimal rate=null,local=null;
        if(currency!=null){
            ExchangeRateQuote quote=rates.rate("USD",currency,LocalDate.now());
            rate=quote.rate();local=request.usdAmount().multiply(rate).setScale("JPY".equals(currency)?0:2,RoundingMode.HALF_UP);
        }
        Instant now=Instant.now();trip.setCashExchangePlanned(true);trip.setCashExchangeMethod(request.method());
        trip.setCashExchangeAmountUsd(request.usdAmount());trip.setCashExchangeLocation(request.plannedLocation());
        trip.setCashExchangePlannedAt(now);trip.setUpdatedAt(now);trips.save(trip);
        audit.log(customer,"CASH_EXCHANGE_PLAN_RECORDED","TRIP",id,request.method()+" · "+request.plannedLocation());
        String availability=ATM.getOrDefault(trip.getDestinationCountry(),"Check bank-owned ATM availability on arrival.");
        String fees="Use a bank-owned ATM, decline dynamic currency conversion and choose the local currency.";
        return new CashExchangePlanResponse(id,currency,request.usdAmount(),local,rate,request.method(),request.plannedLocation(),availability,fees,now);
    }
}
