package com.travelassistant.rule.fraud;

import com.travelassistant.dto.ApiDtos.FraudRuleResult;
import com.travelassistant.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.travelassistant.model.Enums.*;

abstract class BaseFraudRule {
    private final int points;
    BaseFraudRule(int points){this.points=points;}
    FraudRuleResult result(String code, boolean hit, String customer, String internal){
        int p=hit?points:0; RiskLevel l=p>=75?RiskLevel.CRITICAL:p>=50?RiskLevel.HIGH:p>=25?RiskLevel.MEDIUM:RiskLevel.LOW;
        return new FraudRuleResult(code,hit,p,l,customer,internal);
    }
}
@Component class OutsideTripDateRule extends BaseFraudRule implements FraudRule {
    OutsideTripDateRule(@Value("${fraud.thresholds.outside-trip-date:30}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        LocalDate d=t.getTransactionTime().atZone(ZoneOffset.UTC).toLocalDate();
        boolean hit=c.activeTrip()==null||d.isBefore(c.activeTrip().getStartDate())||d.isAfter(c.activeTrip().getEndDate());
        return result("OUTSIDE_TRIP_DATE",hit,"Transaction happened outside your registered travel dates.","event date outside trip");
    }}
@Component class OutsideDestinationRule extends BaseFraudRule implements FraudRule {
    OutsideDestinationRule(@Value("${fraud.thresholds.outside-destination:35}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        boolean hit=c.activeTrip()!=null&&!c.activeTrip().getDestinationCountry().equalsIgnoreCase(t.getMerchantCountry());
        return result("OUTSIDE_DESTINATION",hit,"Transaction country differs from your trip destination.","country mismatch");
    }}
@Component class UnexpectedCurrencyRule extends BaseFraudRule implements FraudRule {
    private static final Map<String,String>C=Map.of("Japan","JPY","France","EUR","Singapore","SGD","China","CNY");
    UnexpectedCurrencyRule(@Value("${fraud.thresholds.unexpected-currency:15}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        String expected=c.activeTrip()==null?null:C.get(c.activeTrip().getDestinationCountry());
        return result("UNEXPECTED_CURRENCY",expected!=null&&!expected.equalsIgnoreCase(t.getOriginalCurrency()),"Payment currency is unusual for this destination.","currency mismatch");
    }}
@Component class DuplicateTransactionRule extends BaseFraudRule implements FraudRule {
    DuplicateTransactionRule(@Value("${fraud.thresholds.duplicate-transaction:40}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        boolean hit=c.recentTransactions().stream().anyMatch(x->!x.getTransactionId().equals(t.getTransactionId())&&
                Objects.equals(x.getMerchantName(),t.getMerchantName())&&x.getOriginalAmount().compareTo(t.getOriginalAmount())==0&&
                Math.abs(Duration.between(x.getTransactionTime(),t.getTransactionTime()).toMinutes())<=10);
        return result("DUPLICATE_TRANSACTION",hit,"This looks like a duplicate charge.","same merchant and amount within 10 minutes");
    }}
@Component class HighValueTransactionRule extends BaseFraudRule implements FraudRule {
    HighValueTransactionRule(@Value("${fraud.thresholds.high-value:30}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        BigDecimal avg=c.customerAverageTransactionAmount()==null?new BigDecimal("200"):c.customerAverageTransactionAmount();
        boolean hit=t.getBillingAmount()!=null&&(t.getBillingAmount().compareTo(new BigDecimal("1000"))>0||t.getBillingAmount().compareTo(avg.multiply(BigDecimal.valueOf(3)))>0);
        return result("HIGH_VALUE_TRANSACTION",hit,"This payment is much larger than usual.","amount exceeds configured baseline");
    }}
@Component class UnusualAtmWithdrawalRule extends BaseFraudRule implements FraudRule {
    UnusualAtmWithdrawalRule(@Value("${fraud.thresholds.unusual-atm:45}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        long n=c.recentTransactions().stream().filter(x->x.getTransactionType()==TransactionType.ATM_WITHDRAWAL&&Duration.between(x.getTransactionTime(),t.getTransactionTime()).abs().toHours()<2).count();
        return result("UNUSUAL_ATM_WITHDRAWAL",t.getTransactionType()==TransactionType.ATM_WITHDRAWAL&&n>=2,"Several ATM withdrawals occurred close together.","ATM velocity");
    }}
@Component class RapidCountryChangeRule extends BaseFraudRule implements FraudRule {
    RapidCountryChangeRule(@Value("${fraud.thresholds.rapid-country-change:60}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        boolean hit=c.recentTransactions().stream().anyMatch(x->!Objects.equals(x.getMerchantCountry(),t.getMerchantCountry())&&Duration.between(x.getTransactionTime(),t.getTransactionTime()).abs().toHours()<3);
        return result("RAPID_COUNTRY_CHANGE",hit,"Transactions appeared in different countries too quickly.","impossible travel");
    }}
@Component class MultipleDeclinesRule extends BaseFraudRule implements FraudRule {
    MultipleDeclinesRule(@Value("${fraud.thresholds.multiple-declines:30}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        long n=c.recentTransactions().stream().filter(x->x.getStatus()==TransactionStatus.DECLINED&&Duration.between(x.getTransactionTime(),t.getTransactionTime()).abs().toHours()<1).count();
        return result("MULTIPLE_DECLINES",n>=3,"Several payments were declined recently.","decline velocity");
    }}
@Component class DeclineThenApprovalRule extends BaseFraudRule implements FraudRule {
    DeclineThenApprovalRule(@Value("${fraud.thresholds.decline-then-approval:40}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        boolean prior=c.recentTransactions().stream().anyMatch(x->x.getStatus()==TransactionStatus.DECLINED&&Duration.between(x.getTransactionTime(),t.getTransactionTime()).abs().toHours()<1);
        boolean hit=t.getStatus()==TransactionStatus.APPROVED&&prior&&t.getBillingAmount()!=null&&t.getBillingAmount().compareTo(new BigDecimal("500"))>0;
        return result("DECLINE_THEN_APPROVAL",hit,"A large payment followed recent declines.","approval after decline");
    }}
@Component class NewMerchantCategoryRule extends BaseFraudRule implements FraudRule {
    NewMerchantCategoryRule(@Value("${fraud.thresholds.new-category:15}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        boolean hit=!c.recentTransactions().isEmpty()&&c.recentTransactions().stream().noneMatch(x->Objects.equals(x.getMerchantCategory(),t.getMerchantCategory()));
        return result("NEW_MERCHANT_CATEGORY",hit,"This merchant category is new for you.","unseen category");
    }}
@Component class CardFrozenTransactionRule extends BaseFraudRule implements FraudRule {
    CardFrozenTransactionRule(@Value("${fraud.thresholds.frozen-card:80}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        return result("CARD_FROZEN_TRANSACTION",c.card().getStatus()==CardStatus.FROZEN,"Activity appeared on a frozen card.","frozen card event");
    }}
@Component class HighRiskCountryRule extends BaseFraudRule implements FraudRule {
    private static final Set<String> HIGH=Set.of("North Korea","Iran","Syria");
    HighRiskCountryRule(@Value("${fraud.thresholds.high-risk-country:60}")int p){super(p);}
    public FraudRuleResult evaluate(TravelTransaction t,FraudContext c){
        return result("HIGH_RISK_COUNTRY",HIGH.contains(t.getMerchantCountry()),"Transaction occurred in a higher-risk location.","configured high-risk country");
    }}
