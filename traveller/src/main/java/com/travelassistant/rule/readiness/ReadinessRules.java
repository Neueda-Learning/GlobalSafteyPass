package com.travelassistant.rule.readiness;

import com.travelassistant.dto.ApiDtos.ReadinessRuleResult;
import com.travelassistant.model.*;
import com.travelassistant.repository.CardRepository;
import com.travelassistant.service.CardCapabilityService;
import org.springframework.stereotype.Component;
import java.math.*;
import java.time.YearMonth;
import java.util.*;
import static com.travelassistant.model.Enums.*;

final class RuleResult {
    static ReadinessRuleResult ok(String code, String message) { return new ReadinessRuleResult(code,true,Severity.INFO,0,message,null); }
    static ReadinessRuleResult fail(String code, Severity severity, int impact, String message, String action) {
        return new ReadinessRuleResult(code,false,severity,impact,message,action);
    }
}

@Component class CardExpiryRule implements ReadinessRule {
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        boolean valid = YearMonth.of(c.getExpiryYear(),c.getExpiryMonth()).atEndOfMonth().isAfter(t.getEndDate());
        return valid ? RuleResult.ok("CARD_EXPIRY","Card remains valid for the whole trip.")
                : RuleResult.fail("CARD_EXPIRED",Severity.CRITICAL,-40,"Card expires before the trip ends.","Choose another card.");
    }
}
@Component class CardFrozenRule implements ReadinessRule {
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        return c.getStatus()==CardStatus.ACTIVE ? RuleResult.ok("CARD_STATUS","Card is active.")
                : RuleResult.fail("CARD_FROZEN",Severity.CRITICAL,-40,"The selected card is not active.","Unfreeze or choose another card.");
    }
}
@Component class OverseasPaymentRule implements ReadinessRule {
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        return c.isOverseasPaymentsEnabled() ? RuleResult.ok("OVERSEAS_PAYMENT","Overseas payments are enabled.")
                : RuleResult.fail("OVERSEAS_PAYMENT_DISABLED",Severity.CRITICAL,-30,"Overseas card payments are disabled.","Enable overseas payments before travelling.");
    }
}
@Component class OnlinePaymentRule implements ReadinessRule {
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        return c.isOnlinePaymentsEnabled() ? RuleResult.ok("ONLINE_PAYMENT","Online payments are enabled.")
                : RuleResult.fail("ONLINE_PAYMENT_DISABLED",Severity.WARNING,-10,"Online card payments are disabled.","Enable online payments for bookings.");
    }
}
@Component class DailyPaymentLimitRule implements ReadinessRule {
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        BigDecimal expected=t.getBudget().divide(BigDecimal.valueOf(Math.max(1,t.getStartDate().until(t.getEndDate()).getDays()+1)),2,RoundingMode.HALF_UP);
        return c.getDailyPaymentLimit().compareTo(expected)>=0 ? RuleResult.ok("PAYMENT_LIMIT","Daily payment limit looks sufficient.")
                : RuleResult.fail("PAYMENT_LIMIT_LOW",Severity.WARNING,-15,"Daily payment limit may be too low.","Increase your daily payment limit.");
    }
}
@Component class WithdrawalLimitRule implements ReadinessRule {
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        return c.getDailyWithdrawalLimit().compareTo(new BigDecimal("100"))>=0 ? RuleResult.ok("WITHDRAWAL_LIMIT","ATM withdrawal limit is available.")
                : RuleResult.fail("WITHDRAWAL_LIMIT_LOW",Severity.WARNING,-5,"ATM withdrawal limit is very low.","Increase the withdrawal limit if you need cash.");
    }
}
@Component class AvailableBalanceRule implements ReadinessRule {
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        return a.getAvailableBalance().compareTo(t.getBudget())>=0 ? RuleResult.ok("AVAILABLE_BALANCE","Available balance covers the budget.")
                : RuleResult.fail("LOW_AVAILABLE_BALANCE",Severity.WARNING,-20,"Available balance is below the travel budget.","Add funds or reduce the budget.");
    }
}
@Component class AccountStatusRule implements ReadinessRule {
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        return a.getStatus()==AccountStatus.ACTIVE ? RuleResult.ok("ACCOUNT_STATUS","Linked account is active.")
                : RuleResult.fail("ACCOUNT_NOT_ACTIVE",Severity.CRITICAL,-40,"Linked account is not active.","Contact the bank or choose another card.");
    }
}
@Component class CurrencySupportRule implements ReadinessRule {
    private static final Map<String,String> CURRENCY=Map.of("Japan","JPY","France","EUR","Singapore","SGD","United States","USD","United Kingdom","GBP","China","CNY");
    private final CardCapabilityService capabilities;
    CurrencySupportRule(CardCapabilityService capabilities){this.capabilities=capabilities;}
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        String destination=CURRENCY.get(t.getDestinationCountry());
        if(destination==null)return t.isCashExchangePlanned()?RuleResult.ok("CASH_EXCHANGE_PLANNED","Cash exchange on arrival is recorded.")
                :RuleResult.fail("CURRENCY_UNKNOWN",Severity.WARNING,-5,"Destination currency could not be verified.","Switch cards in Trip Details or plan to exchange cash on arrival.");
        boolean supported=capabilities.supportsCurrency(c,destination);
        return supported?RuleResult.ok("CURRENCY_SUPPORT","Preferred card supports "+destination+".")
                :t.isCashExchangePlanned()?RuleResult.ok("CASH_EXCHANGE_PLANNED","Cash exchange on arrival is recorded for "+destination+".")
                :RuleResult.fail("CURRENCY_NOT_SUPPORTED",Severity.WARNING,-5,"Preferred card does not list "+destination+" support.","Switch cards in Trip Details or plan to exchange cash on arrival.");
    }
}
@Component class BackupCardRule implements ReadinessRule {
    private final CardRepository cards;
    BackupCardRule(CardRepository cards){this.cards=cards;}
    public ReadinessRuleResult evaluate(Trip t, Card c, Account a) {
        boolean backup=cards.findByCustomerId(t.getCustomerId()).stream().anyMatch(x->!x.getId().equals(c.getId())&&x.getStatus()==CardStatus.ACTIVE);
        return backup ? RuleResult.ok("BACKUP_CARD","A backup card is available.")
                : RuleResult.fail("NO_BACKUP_CARD",Severity.WARNING,-10,"No active backup card was found.","Take a second payment method.");
    }
}
