package com.travelassistant.service;
import com.travelassistant.dto.ApiDtos.AlertResponse;
import com.travelassistant.exception.*;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Service
public class AlertService {
    private final FraudAlertRepository alerts;private final TransactionRepository txs;private final CardControlService cards;private final AuditService audit;private final SupportCaseService cases;
    public AlertService(FraudAlertRepository alerts,TransactionRepository txs,CardControlService cards,AuditService audit,SupportCaseService cases){this.alerts=alerts;this.txs=txs;this.cards=cards;this.audit=audit;this.cases=cases;}
    public List<AlertResponse> list(String c){return alerts.findByCustomerIdOrderByCreatedAtDesc(c).stream().map(this::map).toList();}
    public AlertResponse get(String c,String id){return map(owned(c,id));}
    @Transactional public AlertResponse confirm(String c,String id){FraudAlert a=owned(c,id);a.setStatus(Enums.AlertStatus.CONFIRMED_SAFE);a.setCustomerResponse(Enums.CustomerResponse.CONFIRM);a.setResolvedAt(Instant.now());alerts.save(a);audit.log(c,"TRANSACTION_CONFIRMED","ALERT",id,"Safe");return map(a);}
    @Transactional public AlertResponse report(String c,String id){FraudAlert a=owned(c,id);a.setStatus(Enums.AlertStatus.REPORTED_FRAUD);a.setCustomerResponse(Enums.CustomerResponse.REPORT);a.setResolvedAt(Instant.now());TravelTransaction t=txs.findById(a.getTransactionId()).orElseThrow();t.setDisputed(true);t.setFraudCaseReference("CASE-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());txs.save(t);cases.openFraudCase(c,t);alerts.save(a);audit.log(c,"FRAUD_REPORTED","ALERT",id,t.getFraudCaseReference());return map(a);}
    @Transactional public AlertResponse freeze(String c,String id){FraudAlert a=owned(c,id);cards.freeze(c,a.getCardId());a.setStatus(Enums.AlertStatus.CARD_FROZEN);a.setCustomerResponse(Enums.CustomerResponse.FREEZE);a.setResolvedAt(Instant.now());alerts.save(a);return map(a);}
    private FraudAlert owned(String c,String id){FraudAlert a=alerts.findById(id).orElseThrow(()->new ResourceNotFoundException("Alert not found."));if(!a.getCustomerId().equals(c))throw new ForbiddenException("Alert does not belong to current customer.");return a;}
    private AlertResponse map(FraudAlert a){
        TravelTransaction t=txs.findById(a.getTransactionId()).orElse(null);Card card=cards.owned(a.getCustomerId(),a.getCardId());
        return new AlertResponse(a.getId(),a.getTransactionId(),a.getCardId(),a.getRiskScore(),a.getRiskLevel(),a.getDecision(),a.getReasonCodes()==null?List.of():Arrays.asList(a.getReasonCodes().split(",")),a.getCustomerMessage(),a.getStatus(),a.getCreatedAt(),a.getCustomerResponse(),
                t==null?"Unknown merchant":t.getMerchantName(),t==null?null:t.getMerchantCountry(),t==null?null:t.getOriginalAmount(),t==null?null:t.getOriginalCurrency(),t==null?null:t.getTransactionTime(),card.getMaskedCardNumber(),t==null?null:t.getFraudCaseReference());}
}
