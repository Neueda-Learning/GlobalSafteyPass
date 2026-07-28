package com.travelassistant.integration;
import com.travelassistant.model.*;
import com.travelassistant.repository.CardRepository;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
@Component
public class LocalCardSystemClient implements CardSystemClient {
    private final CardRepository repository;
    public LocalCardSystemClient(CardRepository repository) { this.repository = repository; }
    public Card setOverseasPayments(Card c, boolean enabled){ c.setOverseasPaymentsEnabled(enabled); return repository.save(c); }
    public Card setOnlinePayments(Card c, boolean enabled){ c.setOnlinePaymentsEnabled(enabled); return repository.save(c); }
    public Card setPaymentLimit(Card c, BigDecimal v){ c.setDailyPaymentLimit(v); return repository.save(c); }
    public Card setWithdrawalLimit(Card c, BigDecimal v){ c.setDailyWithdrawalLimit(v); return repository.save(c); }
    public Card freeze(Card c){ c.setStatus(Enums.CardStatus.FROZEN); return repository.save(c); }
    public Card unfreeze(Card c){ c.setStatus(Enums.CardStatus.ACTIVE); return repository.save(c); }
}
