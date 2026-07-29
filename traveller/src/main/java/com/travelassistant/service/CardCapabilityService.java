package com.travelassistant.service;

import com.travelassistant.model.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CardCapabilityService {
    public List<String> supportedCurrencies(Card card){
        return card.getSupportedCurrencies()==null||card.getSupportedCurrencies().isBlank()
                ?List.of():Arrays.stream(card.getSupportedCurrencies().split(","))
                .map(String::trim)
                .filter(s->!s.isBlank())
                .map(s->s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }
    public boolean supportsCurrency(Card card,String currency){
        if(currency==null||currency.isBlank())return true;
        String requested=currency.trim().toUpperCase(Locale.ROOT);
        String main=card.getMainCurrency()==null?"":card.getMainCurrency().trim().toUpperCase(Locale.ROOT);
        return requested.equals(main)||supportedCurrencies(card).contains(requested);
    }
    public boolean canCompleteTravelPayment(Card card,Account account,BigDecimal amount,String destinationCurrency){
        return card.getStatus()==Enums.CardStatus.ACTIVE&&card.isOverseasPaymentsEnabled()
                &&account.getStatus()==Enums.AccountStatus.ACTIVE
                &&account.getAvailableBalance().compareTo(amount)>=0&&supportsCurrency(card,destinationCurrency);
    }
}
