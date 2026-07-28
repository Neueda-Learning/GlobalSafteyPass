package com.travelassistant.service;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.exception.*;
import com.travelassistant.integration.CardSystemClient;
import com.travelassistant.model.Card;
import com.travelassistant.repository.CardRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
@Service
public class CardControlService {
    private final CardRepository cards;private final CardSystemClient client;private final AuditService audit;
    public CardControlService(CardRepository cards,CardSystemClient client,AuditService audit){this.cards=cards;this.client=client;this.audit=audit;}
    public List<CardResponse> list(String customer){return cards.findByCustomerId(customer).stream().map(this::map).toList();}
    public CardResponse overseas(String customer,String id,boolean enabled){Card c=owned(customer,id);client.setOverseasPayments(c,enabled);audit.log(customer,enabled?"OVERSEAS_PAYMENT_ENABLED":"OVERSEAS_PAYMENT_DISABLED","CARD",id,"enabled="+enabled);return map(c);}
    public CardResponse online(String customer,String id,boolean enabled){Card c=owned(customer,id);client.setOnlinePayments(c,enabled);audit.log(customer,enabled?"ONLINE_PAYMENT_ENABLED":"ONLINE_PAYMENT_DISABLED","CARD",id,"enabled="+enabled);return map(c);}
    public CardResponse freeze(String customer,String id){Card c=owned(customer,id);client.freeze(c);audit.log(customer,"CARD_FROZEN","CARD",id,"Card frozen");return map(c);}
    public CardResponse unfreeze(String customer,String id){Card c=owned(customer,id);client.unfreeze(c);audit.log(customer,"CARD_UNFROZEN","CARD",id,"Card unfrozen");return map(c);}
    public CardResponse paymentLimit(String customer,String id,BigDecimal v){Card c=owned(customer,id);client.setPaymentLimit(c,v);audit.log(customer,"CARD_LIMIT_CHANGED","CARD",id,"payment="+v);return map(c);}
    public CardResponse withdrawalLimit(String customer,String id,BigDecimal v){Card c=owned(customer,id);client.setWithdrawalLimit(c,v);audit.log(customer,"CARD_LIMIT_CHANGED","CARD",id,"withdrawal="+v);return map(c);}
    public Card owned(String customer,String id){Card c=cards.findById(id).orElseThrow(()->new ResourceNotFoundException("Card not found."));if(!c.getCustomerId().equals(customer))throw new ForbiddenException("Card does not belong to current customer.");return c;}
    private CardResponse map(Card c){return new CardResponse(c.getId(),c.getMaskedCardNumber(),c.getCardType(),c.getStatus(),c.isOverseasPaymentsEnabled(),c.isOnlinePaymentsEnabled(),c.getDailyPaymentLimit(),c.getDailyWithdrawalLimit(),c.getMainCurrency(),c.getSupportedCurrencies()==null||c.getSupportedCurrencies().isBlank()?List.of():List.of(c.getSupportedCurrencies().split(",")),c.getExpiryMonth(),c.getExpiryYear());}
}
