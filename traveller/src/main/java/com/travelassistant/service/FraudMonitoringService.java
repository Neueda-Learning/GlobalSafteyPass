package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.integration.FraudDetectionClient;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import com.travelassistant.rule.fraud.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class FraudMonitoringService {
    private static final Logger log=LoggerFactory.getLogger(FraudMonitoringService.class);
    private final List<FraudRule> rules;private final FraudDetectionClient external;private final FraudAlertRepository alerts;
    private final AuditService audit;private final int threshold;
    private final double internalWeight,externalWeight,profileWeight;
    public FraudMonitoringService(List<FraudRule> rules,FraudDetectionClient external,FraudAlertRepository alerts,AuditService audit,
            @Value("${fraud.alert-threshold:50}")int threshold,
            @Value("${fraud.weights.internal:0.50}")double internalWeight,
            @Value("${fraud.weights.external:0.30}")double externalWeight,
            @Value("${fraud.weights.customer-profile:0.20}")double profileWeight){
        this.rules=rules;this.external=external;this.alerts=alerts;this.audit=audit;this.threshold=threshold;
        this.internalWeight=internalWeight;this.externalWeight=externalWeight;this.profileWeight=profileWeight;}
    public FraudAssessment evaluate(TravelTransaction t,FraudContext c){
        List<FraudRuleResult> rr=rules.stream().map(r->r.evaluate(t,c)).toList();
        int internal=Math.min(100,rr.stream().filter(FraudRuleResult::triggered).mapToInt(FraudRuleResult::riskPoints).sum());
        ExternalFraudResult er;try{er=external.score(t,c);}catch(Exception e){log.warn("External fraud scoring unavailable",e);er=new ExternalFraudResult(null,List.of(),null,"unavailable");}
        int profile=profileScore(t,c);
        int score=er.available()
                ?Math.min(100,(int)Math.round(internal*internalWeight+er.riskScore()*externalWeight+profile*profileWeight))
                :Math.min(100,(int)Math.round(internal*.70+profile*.30));
        Enums.RiskLevel level=score>=75?Enums.RiskLevel.CRITICAL:score>=50?Enums.RiskLevel.HIGH:score>=25?Enums.RiskLevel.MEDIUM:Enums.RiskLevel.LOW;
        Enums.FraudDecision decision=switch(level){case LOW->Enums.FraudDecision.ALLOW;case MEDIUM->Enums.FraudDecision.MONITOR;case HIGH->Enums.FraudDecision.REQUIRE_CONFIRMATION;case CRITICAL->Enums.FraudDecision.BLOCK_AND_ALERT;};
        List<String> reasons=new ArrayList<>(rr.stream().filter(FraudRuleResult::triggered).map(FraudRuleResult::ruleCode).toList());
        if(profile>=50)reasons.add("CUSTOMER_PROFILE_ANOMALY");
        if(score>=threshold){
            FraudAlert a=FraudAlert.builder().id("alert-"+UUID.randomUUID()).customerId(t.getCustomerId()).tripId(t.getTripId())
                    .transactionId(t.getTransactionId()).cardId(t.getCardId()).riskScore(score).riskLevel(level).decision(decision)
                    .reasonCodes(String.join(",",reasons)).customerMessage("We noticed unusual travel card activity. Please review it.")
                    .status(Enums.AlertStatus.OPEN).createdAt(Instant.now()).customerResponse(Enums.CustomerResponse.NONE).build();
            alerts.save(a);audit.log(t.getCustomerId(),"FRAUD_ALERT_CREATED","ALERT",a.getId(),String.join(",",reasons));
        }
        return new FraudAssessment(internal,er.riskScore(),profile,score,level,decision,reasons);
    }
    private int profileScore(TravelTransaction t,FraudContext c){
        int score=0;BigDecimal average=c.customerAverageTransactionAmount();
        if(average!=null&&average.signum()>0&&t.getBillingAmount()!=null&&t.getBillingAmount().compareTo(average.multiply(BigDecimal.valueOf(4)))>0)score+=45;
        if(!c.customerKnownCountries().isEmpty()&&!c.customerKnownCountries().contains(t.getMerchantCountry()))score+=30;
        if(t.getMerchantCategory()!=null&&!c.customerKnownMerchantCategories().isEmpty()&&!c.customerKnownMerchantCategories().contains(t.getMerchantCategory()))score+=20;
        if(t.getTransactionType()==Enums.TransactionType.ATM_WITHDRAWAL&&c.recentTransactions().stream().noneMatch(x->x.getTransactionType()==Enums.TransactionType.ATM_WITHDRAWAL))score+=15;
        return Math.min(100,score);
    }
}
