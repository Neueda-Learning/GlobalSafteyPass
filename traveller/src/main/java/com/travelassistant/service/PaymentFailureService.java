package com.travelassistant.service;
import com.travelassistant.dto.ApiDtos.FailureExplanation;
import com.travelassistant.model.Enums.ActionType;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class PaymentFailureService {
    private record Template(String title,String message,String action,ActionType type){}
    private static final Map<String,Template> M=Map.ofEntries(
        Map.entry("INSUFFICIENT_FUNDS",new Template("Not enough funds","Your available balance is too low for this payment.","Add funds or use another card.",ActionType.ADD_FUNDS)),
        Map.entry("CARD_FROZEN",new Template("Card is frozen","This card is currently frozen.","Unfreeze the card if this payment is yours.",ActionType.UNFREEZE_CARD)),
        Map.entry("CARD_EXPIRED",new Template("Card expired","This card has expired.","Use another card.",ActionType.USE_ANOTHER_CARD)),
        Map.entry("OVERSEAS_DISABLED",new Template("Overseas payments disabled","International payments are switched off.","Enable overseas payments.",ActionType.ENABLE_OVERSEAS_PAYMENT)),
        Map.entry("ONLINE_PAYMENT_DISABLED",new Template("Online payments disabled","Online payments are switched off.","Enable online payments.",ActionType.ENABLE_ONLINE_PAYMENT)),
        Map.entry("LIMIT_EXCEEDED",new Template("Payment limit reached","This payment exceeds your current daily card limit.","Increase your payment limit or use another card.",ActionType.INCREASE_LIMIT)),
        Map.entry("ATM_LIMIT_EXCEEDED",new Template("ATM limit reached","This withdrawal exceeds your daily ATM limit.","Increase the withdrawal limit or try a smaller amount.",ActionType.INCREASE_LIMIT)),
        Map.entry("FRAUD_BLOCK",new Template("Payment needs confirmation","We paused this payment for your security.","Confirm the transaction or contact the bank.",ActionType.CONFIRM_TRANSACTION)),
        Map.entry("INVALID_PIN",new Template("PIN was not accepted","The PIN entered was incorrect.","Check your PIN and retry carefully.",ActionType.RETRY)),
        Map.entry("MERCHANT_NOT_SUPPORTED",new Template("Merchant not supported","This merchant cannot accept this card.","Use another payment method.",ActionType.USE_ANOTHER_CARD)),
        Map.entry("NETWORK_ERROR",new Template("Connection problem","The payment network did not respond.","Wait a moment and retry.",ActionType.RETRY)),
        Map.entry("DO_NOT_HONOR",new Template("Payment not approved","The bank could not approve this payment.","Try another card or contact the bank.",ActionType.CONTACT_BANK)));
    public FailureExplanation explain(String id,String code){String c=code==null?"UNKNOWN":code;Template t=M.getOrDefault(c,new Template("Payment declined","We could not complete this payment.","Contact the bank if the issue continues.",ActionType.CONTACT_BANK));return new FailureExplanation(id,c,t.title,t.message,t.action,t.type);}
}
