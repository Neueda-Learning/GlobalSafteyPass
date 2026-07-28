package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class PaymentRecoveryService {
    private static final Map<String,String> DESTINATION_CURRENCY=Map.of("Japan","JPY","France","EUR","Singapore","SGD","United Kingdom","GBP","China","CNY","United States","USD");
    private final TransactionService transactions;private final TransactionRepository repository;
    private final CardRepository cards;private final AccountRepository accounts;private final TripRepository trips;
    private final FraudAlertRepository alerts;private final AuditService audit;private final CardCapabilityService capabilities;
    private final TravelCardRecommendationService recommendations;
    public PaymentRecoveryService(TransactionService transactions,TransactionRepository repository,CardRepository cards,
            AccountRepository accounts,TripRepository trips,FraudAlertRepository alerts,AuditService audit,
            CardCapabilityService capabilities,TravelCardRecommendationService recommendations){
        this.transactions=transactions;this.repository=repository;this.cards=cards;this.accounts=accounts;this.trips=trips;this.alerts=alerts;this.audit=audit;
        this.capabilities=capabilities;this.recommendations=recommendations;}

    public PaymentRecoveryResponse get(String customer,String id){return response(customer,transactions.owned(customer,id));}

    @Transactional public PaymentRecoveryResponse retry(String customer,String id){
        TravelTransaction t=transactions.owned(customer,id);
        if(t.getRecoveryStatus()==Enums.RecoveryStatus.COMPLETED_AFTER_RETRY||t.getRecoveryStatus()==Enums.RecoveryStatus.COMPLETED_WITH_ALTERNATE_CARD)return response(customer,t);
        t.setRecoveryAttemptCount(t.getRecoveryAttemptCount()+1);t.setRecoveryStartedAt(Instant.now());
        t.setRecoveryStatus(Enums.RecoveryStatus.RECOVERY_IN_PROGRESS);t.setRecoveryUpdatedAt(Instant.now());repository.save(t);
        if("NETWORK_ERROR".equals(t.getFailureCode())){
            t.setStatus(Enums.TransactionStatus.APPROVED);t.setRecoveryStatus(Enums.RecoveryStatus.COMPLETED_AFTER_RETRY);
            t.setRecoveryCompletedAt(Instant.now());t.setRecoveryUpdatedAt(Instant.now());repository.save(t);
            audit.log(customer,"PAYMENT_RECOVERED","TRANSACTION",id,"Original authorization completed after network retry");
        }else{
            t.setRecoveryStatus(Enums.RecoveryStatus.ACTION_REQUIRED);t.setRecoveryUpdatedAt(Instant.now());repository.save(t);
            audit.log(customer,"PAYMENT_RECOVERY_ACTION_REQUIRED","TRANSACTION",id,t.getFailureCode());
        }
        return response(customer,t);
    }

    @Transactional public PaymentRecoveryResponse useAlternateCard(String customer,String id,String alternateCardId){
        TravelTransaction t=transactions.owned(customer,id);
        Card alternate=cards.findById(alternateCardId).orElseThrow(()->new IllegalArgumentException("Alternate card was not found."));
        if(!alternate.getCustomerId().equals(customer))throw new IllegalArgumentException("This card does not belong to the customer.");
        if(alternate.getId().equals(t.getCardId()))throw new IllegalArgumentException("Choose a different card.");
        Account account=accounts.findById(alternate.getLinkedAccountId()).orElseThrow();
        Trip trip=t.getTripId()==null?null:trips.findById(t.getTripId()).orElse(null);
        String currency=trip==null?null:DESTINATION_CURRENCY.get(trip.getDestinationCountry());
        boolean securityCleared=alerts.findByTransactionId(t.getTransactionId()).stream()
                .noneMatch(a->a.getStatus()==Enums.AlertStatus.OPEN||a.getStatus()==Enums.AlertStatus.REPORTED_FRAUD);
        if(!securityCleared)throw new IllegalArgumentException("Review and confirm the security alert before using another card.");
        if(!capabilities.canCompleteTravelPayment(alternate,account,t.getBillingAmount(),currency))
            throw new IllegalArgumentException("This card is no longer eligible. Check status, overseas payments, funds and currency support.");
        t.setRecoveryAttemptCount(t.getRecoveryAttemptCount()+1);t.setRecoveryStartedAt(Instant.now());
        t.setRecoveryStatus(Enums.RecoveryStatus.RECOVERY_IN_PROGRESS);t.setRecoveryUpdatedAt(Instant.now());repository.save(t);
        t.setCardId(alternate.getId());t.setStatus(Enums.TransactionStatus.APPROVED);
        t.setRecoveryStatus(Enums.RecoveryStatus.COMPLETED_WITH_ALTERNATE_CARD);
        t.setRecoveryCompletedAt(Instant.now());t.setRecoveryUpdatedAt(Instant.now());repository.save(t);
        audit.log(customer,"PAYMENT_RECOVERED_WITH_ALTERNATE_CARD","TRANSACTION",id,"card="+alternate.getMaskedCardNumber());
        return response(customer,t);
    }

    private PaymentRecoveryResponse response(String customer,TravelTransaction t){
        Card card=cards.findById(t.getCardId()).orElseThrow();Account account=accounts.findById(card.getLinkedAccountId()).orElseThrow();
        Trip trip=t.getTripId()==null?null:trips.findById(t.getTripId()).orElse(null);
        boolean locationMatch=trip!=null&&countryMatches(trip.getDestinationCountry(),t.getMerchantCountry());
        boolean withinTrip=trip!=null&&!t.getTransactionTime().atZone(ZoneOffset.UTC).toLocalDate().isBefore(trip.getStartDate())
                &&!t.getTransactionTime().atZone(ZoneOffset.UTC).toLocalDate().isAfter(trip.getEndDate());
        boolean noFraud=alerts.findByTransactionId(t.getTransactionId()).stream().noneMatch(a->a.getStatus()==Enums.AlertStatus.OPEN||a.getStatus()==Enums.AlertStatus.REPORTED_FRAUD);
        boolean funds=account.getAvailableBalance().compareTo(t.getBillingAmount())>=0;
        boolean cardActive=card.getStatus()==Enums.CardStatus.ACTIVE;
        List<RecoveryCheck> checks=List.of(
                new RecoveryCheck("Card is active",cardActive,cardActive?"Your card is working normally.":"The card must be reactivated or replaced."),
                new RecoveryCheck("Balance available",funds,funds?"There are enough available funds for this payment.":"Available funds are below the payment amount."),
                new RecoveryCheck("Travel location matches",locationMatch&&withinTrip,locationMatch&&withinTrip?t.getMerchantCity()+" payment matches your registered "+trip.getDestinationCity()+" journey.":"The payment location or date needs review."),
                new RecoveryCheck("No fraud issue detected",noFraud,noFraud?"No fraud signal is blocking this payment.":"Review the security alert before retrying."));
        String code=Optional.ofNullable(t.getFailureCode()).orElse("UNKNOWN");
        String explanation=switch(code){
            case "NETWORK_ERROR"->"Your card is working normally. The payment did not complete because the merchant’s payment network did not respond.";
            case "LIMIT_EXCEEDED"->"The payment is above the card’s current daily payment limit.";
            case "INSUFFICIENT_FUNDS"->"The linked account does not currently have enough available funds for this payment.";
            case "FRAUD_BLOCK"->"We paused this payment because it needs a security confirmation.";
            default->"The payment could not be completed yet. We checked the card and prepared the safest next step.";};
        String action=switch(code){case "NETWORK_ERROR"->"RETRY_PAYMENT";case "LIMIT_EXCEEDED"->"CHANGE_LIMIT";case "INSUFFICIENT_FUNDS"->"ADD_FUNDS";case "FRAUD_BLOCK"->"REVIEW_SECURITY";default->"CONTACT_SUPPORT";};
        Enums.RecoveryStatus status=t.getRecoveryStatus();
        if(status==null)status=t.getStatus()==Enums.TransactionStatus.APPROVED?Enums.RecoveryStatus.COMPLETED_AFTER_RETRY:
                ("NETWORK_ERROR".equals(code)?Enums.RecoveryStatus.RETRY_AVAILABLE:Enums.RecoveryStatus.ACTION_REQUIRED);
        List<RecoveryTimelineEvent> timeline=new ArrayList<>();
        timeline.add(new RecoveryTimelineEvent(t.getTransactionTime(),"Payment attempted",t.getMerchantName()+" requested authorization.","DONE"));
        timeline.add(new RecoveryTimelineEvent(t.getTransactionTime().plusSeconds(60),"Payment interrupted",friendlyReason(code),"DONE"));
        if(t.getRecoveryStartedAt()!=null)timeline.add(new RecoveryTimelineEvent(t.getRecoveryStartedAt(),"Recovery started","A new authorization was sent using the same payment record.","DONE"));
        if(t.getRecoveryCompletedAt()!=null)timeline.add(new RecoveryTimelineEvent(t.getRecoveryCompletedAt(),"Payment completed",t.getRecoveryStatus()==Enums.RecoveryStatus.COMPLETED_WITH_ALTERNATE_CARD?"Authorization completed with the selected backup card.":"The merchant authorization completed successfully.","DONE"));
        String safety=t.getRecoveryStatus()==Enums.RecoveryStatus.COMPLETED_AFTER_RETRY||t.getRecoveryStatus()==Enums.RecoveryStatus.COMPLETED_WITH_ALTERNATE_CARD
                ?"The original payment is now complete. Only one charge can be posted."
                :"The merchant did not receive money from the interrupted attempt. Retrying cannot charge that failed attempt twice.";
        String travel=trip==null?"No journey was linked to this payment.":locationMatch&&withinTrip
                ?"This "+t.getMerchantCity()+" payment matches your "+trip.getDestinationCity()+" trip."
                :"This payment does not fully match the registered trip, so we recommend a security review.";
        String destinationCurrency=trip==null?null:DESTINATION_CURRENCY.get(trip.getDestinationCountry());
        List<RecoveryCardOption> eligibleCards=noFraud
                ?recommendations.eligibleCards(customer,t.getCardId(),t.getBillingAmount(),destinationCurrency):List.of();
        return new PaymentRecoveryResponse(t.getTransactionId(),status,t.getMerchantName(),t.getBillingAmount(),t.getBillingCurrency(),
                t.getMerchantCity(),t.getMerchantCountry(),card.getMaskedCardNumber(),code,
                "NETWORK_ERROR".equals(code)?92:85,explanation,safety,travel,action,
                List.of("USE_ANOTHER_CARD","CONTACT_MERCHANT","CONTACT_BANK"),eligibleCards,checks,timeline);
    }
    private String friendlyReason(String code){return switch(code){case "NETWORK_ERROR"->"The payment network did not respond.";case "LIMIT_EXCEEDED"->"The daily card limit stopped the payment.";case "INSUFFICIENT_FUNDS"->"Available funds were too low.";case "FRAUD_BLOCK"->"A security confirmation is required.";default->"Authorization did not complete.";};}
    private boolean countryMatches(String destination,String merchant){return destination!=null&&merchant!=null&&(destination.equalsIgnoreCase(merchant)||destination.equals("Japan")&&merchant.equalsIgnoreCase("Japan"));}
}
