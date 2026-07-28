package com.travelassistant.integration;
import com.travelassistant.dto.ApiDtos.ExternalFraudResult;
import com.travelassistant.model.*;
import com.travelassistant.rule.fraud.FraudContext;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.math.BigDecimal;
import java.util.*;
@Component
@ConditionalOnProperty(name="integration.fraud.mode",havingValue="mock",matchIfMissing=true)
public class MockFraudDetectionClient implements FraudDetectionClient {
    public ExternalFraudResult score(TravelTransaction t, FraudContext c) {
        int score=0; List<String> signals=new ArrayList<>();
        if (t.getBillingAmount()!=null && t.getBillingAmount().compareTo(new BigDecimal("1000"))>0){score+=30;signals.add("HIGH_AMOUNT");}
        if (c.activeTrip()!=null && !c.activeTrip().getDestinationCountry().equalsIgnoreCase(t.getMerchantCountry())){score+=30;signals.add("DESTINATION_MISMATCH");}
        if (t.getTransactionType()==Enums.TransactionType.ATM_WITHDRAWAL){score+=10;signals.add("ATM");}
        if (c.customerAverageTransactionAmount()!=null && c.customerAverageTransactionAmount().signum()>0 &&
                t.getBillingAmount()!=null && t.getBillingAmount().compareTo(c.customerAverageTransactionAmount().multiply(BigDecimal.valueOf(4)))>0){
            score+=20;signals.add("CUSTOMER_PROFILE_AMOUNT_ANOMALY");
        }
        return new ExternalFraudResult(Math.min(100,score),signals,UUID.randomUUID().toString(),"MockBankRisk");
    }
}
