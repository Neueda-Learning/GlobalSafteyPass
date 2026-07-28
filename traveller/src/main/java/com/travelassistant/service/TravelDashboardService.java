package com.travelassistant.service;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.springframework.stereotype.Service;
import java.math.*;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class TravelDashboardService {
    private final TripService trips;private final TransactionRepository txs;private final TransactionService mapper;private final ReadinessAssessmentRepository readiness;private final FraudAlertRepository alerts;
    public TravelDashboardService(TripService trips,TransactionRepository txs,TransactionService mapper,ReadinessAssessmentRepository readiness,FraudAlertRepository alerts){this.trips=trips;this.txs=txs;this.mapper=mapper;this.readiness=readiness;this.alerts=alerts;}
    public DashboardResponse get(String customer,String id){
        Trip t=trips.owned(customer,id);List<TravelTransaction> all=txs.findByTripIdOrderByTransactionTimeDesc(id);
        BigDecimal spent=all.stream().filter(x->x.getStatus()==Enums.TransactionStatus.APPROVED).map(x->x.getTransactionType()==Enums.TransactionType.REFUND?x.getBillingAmount().negate():x.getBillingAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);
        Map<String,BigDecimal> cat=all.stream().filter(x->x.getStatus()==Enums.TransactionStatus.APPROVED).collect(Collectors.groupingBy(x->Optional.ofNullable(x.getMerchantCategory()).orElse("OTHER"),Collectors.reducing(BigDecimal.ZERO,x->x.getTransactionType()==Enums.TransactionType.REFUND?x.getBillingAmount().negate():x.getBillingAmount(),BigDecimal::add)));
        BigDecimal remaining=t.getBudget().subtract(spent);BigDecimal pct=t.getBudget().signum()==0?BigDecimal.ZERO:spent.multiply(BigDecimal.valueOf(100)).divide(t.getBudget(),2,RoundingMode.HALF_UP);
        int approved=(int)all.stream().filter(x->x.getStatus()==Enums.TransactionStatus.APPROVED).count(),declined=(int)all.stream().filter(x->x.getStatus()==Enums.TransactionStatus.DECLINED).count();
        return new DashboardResponse(id,t.getDestinationCity()+", "+t.getDestinationCountry(),t.getBudget(),t.getBudgetCurrency(),spent,remaining,pct,all.size(),approved,declined,cat,all.stream().limit(8).map(mapper::map).toList(),readiness.findByTripId(id).map(ReadinessAssessment::getStatus).orElse(null),alerts.countByTripIdAndStatus(id,Enums.AlertStatus.OPEN));
    }
}
