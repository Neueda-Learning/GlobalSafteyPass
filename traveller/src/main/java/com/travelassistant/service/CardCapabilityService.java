package com.travelassistant.service;

import com.travelassistant.model.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class CardCapabilityService {
    public List<String> supportedCurrencies(Card card){
        return card.getSupportedCurrencies()==null||card.getSupportedCurrencies().isBlank()
                ?List.of():Arrays.asList(card.getSupportedCurrencies().split(","));
    }
    public boolean supportsCurrency(Card card,String currency){
        return currency==null||currency.equals(card.getMainCurrency())||supportedCurrencies(card).contains(currency);
    }
    public boolean canCompleteTravelPayment(Card card,Account account,BigDecimal amount,String destinationCurrency){
        return card.getStatus()==Enums.CardStatus.ACTIVE&&card.isOverseasPaymentsEnabled()
                &&account.getStatus()==Enums.AccountStatus.ACTIVE
                &&account.getAvailableBalance().compareTo(amount)>=0&&supportsCurrency(card,destinationCurrency);
    }
}
