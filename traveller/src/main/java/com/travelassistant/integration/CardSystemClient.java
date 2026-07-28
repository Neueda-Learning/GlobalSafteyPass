package com.travelassistant.integration;
import com.travelassistant.model.Card;
import java.math.BigDecimal;
public interface CardSystemClient {
    Card setOverseasPayments(Card card, boolean enabled);
    Card setOnlinePayments(Card card, boolean enabled);
    Card setPaymentLimit(Card card, BigDecimal limit);
    Card setWithdrawalLimit(Card card, BigDecimal limit);
    Card freeze(Card card);
    Card unfreeze(Card card);
}
