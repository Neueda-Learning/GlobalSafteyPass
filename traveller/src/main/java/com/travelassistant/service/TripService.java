package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.exception.*;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class TripService {
    private final TripRepository trips; private final CardRepository cards; private final AuditService audit;
    public TripService(TripRepository trips,CardRepository cards,AuditService audit){this.trips=trips;this.cards=cards;this.audit=audit;}
    @Transactional public TripResponse create(String customer,TripRequest r){
        validate(customer,r);
        Trip t=Trip.builder().id("trip-"+UUID.randomUUID()).customerId(customer).destinationCountry(r.destinationCountry())
                .destinationCity(r.destinationCity()).startDate(r.startDate()).endDate(r.endDate()).budget(r.budget())
                .budgetCurrency(r.budgetCurrency().toUpperCase()).preferredCardId(r.preferredCardId())
                .status(Enums.TripStatus.PLANNED).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        trips.save(t); audit.log(customer,"TRIP_CREATED","TRIP",t.getId(),t.getDestinationCountry()); return map(t);
    }
    public List<TripResponse> list(String customer){return trips.findByCustomerIdOrderByStartDateDesc(customer).stream().map(this::map).toList();}
    public TripResponse get(String customer,String id){return map(owned(customer,id));}
    @Transactional public TripResponse update(String customer,String id,TripRequest r){
        Trip t=owned(customer,id);
        if(t.getStatus()!=Enums.TripStatus.PLANNED)throw new InvalidTripException("Only planned trips can be edited.");
        validate(customer,r);t.setDestinationCountry(r.destinationCountry());t.setDestinationCity(r.destinationCity());
        t.setStartDate(r.startDate());t.setEndDate(r.endDate());t.setBudget(r.budget());t.setBudgetCurrency(r.budgetCurrency().toUpperCase());
        t.setPreferredCardId(r.preferredCardId());
        if(r.currencySettlementMethod()!=null)t.setCurrencySettlementMethod(r.currencySettlementMethod());
        if(r.currencyCheckPassed()!=null)t.setCurrencyCheckPassed(r.currencyCheckPassed());
        t.setUpdatedAt(Instant.now());trips.save(t);audit.log(customer,"TRIP_UPDATED","TRIP",id,"Trip updated");return map(t);
    }
    @Transactional public void delete(String customer,String id){
        Trip t=owned(customer,id);
        if(t.getStatus()!=Enums.TripStatus.PLANNED)throw new InvalidTripException("Only planned trips can be deleted.");
        audit.log(customer,"TRIP_DELETED","TRIP",id,t.getDestinationCountry());
        trips.delete(t);
    }
    public Trip owned(String customer,String id){
        Trip t=trips.findById(id).orElseThrow(()->new ResourceNotFoundException("Trip not found."));
        if(!t.getCustomerId().equals(customer))throw new ForbiddenException("Trip does not belong to current customer."); return t;
    }
    private void validate(String customer,TripRequest r){
        if(r.endDate().isBefore(r.startDate()))throw new InvalidTripException("Trip end date must be on or after start date.");
        Card c=cards.findById(r.preferredCardId()).orElseThrow(()->new ResourceNotFoundException("Preferred card not found."));
        if(!c.getCustomerId().equals(customer))throw new ForbiddenException("Card does not belong to current customer.");
    }
    public TripResponse map(Trip t){return new TripResponse(t.getId(),t.getDestinationCountry(),t.getDestinationCity(),t.getStartDate(),t.getEndDate(),t.getBudget(),t.getBudgetCurrency(),t.getPreferredCardId(),t.getStatus(),t.isCashExchangePlanned(),t.getCashExchangeMethod(),t.getCashExchangeAmountUsd(),t.getCashExchangeLocation(),t.getCashExchangePlannedAt(),t.getCurrencySettlementMethod(),t.isCurrencyCheckPassed());}
}
