package com.travelassistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.exception.ResourceNotFoundException;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import com.travelassistant.rule.readiness.ReadinessRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class ReadinessService {
    private static final String USD_SETTLEMENT_OPTION = "USD_SETTLEMENT";
    private static final String ATM_CASH_OPTION = "ATM_CASH";
    private final TripService tripService; private final CardRepository cards; private final AccountRepository accounts;
    private final List<ReadinessRule> rules; private final ReadinessAssessmentRepository assessments;
    private final ObjectMapper mapper; private final AuditService audit;
    public ReadinessService(TripService ts,CardRepository c,AccountRepository a,List<ReadinessRule> rules,
            ReadinessAssessmentRepository ar,ObjectMapper mapper,AuditService audit){
        this.tripService=ts;this.cards=c;this.accounts=a;this.rules=rules;this.assessments=ar;this.mapper=mapper;this.audit=audit;}
    @Transactional public ReadinessResponse check(String customer,String tripId){
        Trip trip=tripService.owned(customer,tripId); Card card=cards.findById(trip.getPreferredCardId()).orElseThrow();
        Account account=accounts.findById(card.getLinkedAccountId()).orElseThrow();
        List<ReadinessRuleResult> checks=rules.stream().map(r->r.evaluate(trip,card,account)).toList();
        int score=Math.max(0,Math.min(100,100+checks.stream().mapToInt(ReadinessRuleResult::scoreImpact).sum()));
        Enums.ReadinessStatus status=score>=80?Enums.ReadinessStatus.READY:score>=50?Enums.ReadinessStatus.ACTION_REQUIRED:Enums.ReadinessStatus.NOT_READY;
        try{
            ReadinessAssessment x=assessments.findByTripId(tripId).orElseGet(()->ReadinessAssessment.builder().id(UUID.randomUUID().toString()).tripId(tripId).build());
            x.setScore(score);x.setStatus(status);x.setChecksJson(mapper.writeValueAsString(checks));x.setCheckedAt(Instant.now());assessments.save(x);
        }catch(Exception e){throw new IllegalStateException("Could not store readiness assessment",e);}
        audit.log(customer,"READINESS_CHECK_COMPLETED","TRIP",tripId,"score="+score);return new ReadinessResponse(tripId,score,status,checks);
    }
    public ReadinessResponse get(String customer,String tripId){
        tripService.owned(customer,tripId);
        return assessments.findByTripId(tripId).map(x->{try{return new ReadinessResponse(tripId,x.getScore(),x.getStatus(),mapper.readValue(x.getChecksJson(),new TypeReference<>(){}));}catch(Exception e){throw new IllegalStateException(e);}})
                .orElseThrow(()->new ResourceNotFoundException("Run a readiness check first."));
    }
    @Transactional public void setCurrencyFallback(String customer,String tripId,String option){
        Trip trip=tripService.owned(customer,tripId);
        String normalized=(option==null?"":option.trim().toUpperCase(Locale.ROOT));
        if(!USD_SETTLEMENT_OPTION.equals(normalized)&&!ATM_CASH_OPTION.equals(normalized)){
            throw new IllegalArgumentException("Unsupported currency fallback option.");
        }
        Instant now=Instant.now();
        trip.setCashExchangePlanned(true);
        trip.setCashExchangeMethod(normalized);
        trip.setCashExchangePlannedAt(now);
        trip.setCashExchangeLocation(ATM_CASH_OPTION.equals(normalized)
                ?(trip.getDestinationCity()==null||trip.getDestinationCity().isBlank()?trip.getDestinationCountry():trip.getDestinationCity()+", "+trip.getDestinationCountry())
                :"USD settlement");
        trip.setCashExchangeAmountUsd(null);
        trip.setUpdatedAt(now);
        String detail=USD_SETTLEMENT_OPTION.equals(normalized)
                ?"Accepted USD settlement fallback"
                :"Will use ATM cash withdrawal to avoid USD settlement";
        audit.log(customer,"READINESS_CURRENCY_FALLBACK_SET","TRIP",tripId,detail);
    }
}
