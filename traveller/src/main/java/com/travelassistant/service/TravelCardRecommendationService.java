package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.RecoveryCardOption;
import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class TravelCardRecommendationService {
    private final CardRepository cards;private final AccountRepository accounts;private final CardCapabilityService capabilities;
    public TravelCardRecommendationService(CardRepository cards,AccountRepository accounts,CardCapabilityService capabilities){
        this.cards=cards;this.accounts=accounts;this.capabilities=capabilities;}
    public List<RecoveryCardOption> eligibleCards(String customer,String currentCardId,BigDecimal amount,String destinationCurrency){
        return cards.findByCustomerId(customer).stream().filter(c->!c.getId().equals(currentCardId))
                .filter(c->accounts.findById(c.getLinkedAccountId()).map(a->capabilities.canCompleteTravelPayment(c,a,amount,destinationCurrency)).orElse(false))
                .map(c->new RecoveryCardOption(c.getId(),c.getMaskedCardNumber(),c.getCardType(),c.getMainCurrency(),
                        capabilities.supportedCurrencies(c),"Active, sufficient funds and ready for "+Optional.ofNullable(destinationCurrency).orElse("this destination")))
                .toList();
    }
}
