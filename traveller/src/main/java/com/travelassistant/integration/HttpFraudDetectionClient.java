package com.travelassistant.integration;

import com.travelassistant.dto.ApiDtos.ExternalFraudResult;
import com.travelassistant.model.TravelTransaction;
import com.travelassistant.rule.fraud.FraudContext;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Component
@ConditionalOnProperty(name="integration.fraud.mode",havingValue="http")
public class HttpFraudDetectionClient implements FraudDetectionClient {
    private final WebClient client; private final String apiKey;
    public HttpFraudDetectionClient(@Qualifier("fraudWebClient") WebClient client,
            @Value("${integration.fraud.api-key:}") String apiKey){this.client=client;this.apiKey=apiKey;}
    public ExternalFraudResult score(TravelTransaction t,FraudContext c){
        return client.post().uri("/score").contentType(MediaType.APPLICATION_JSON)
                .headers(h->{if(!apiKey.isBlank())h.setBearerAuth(apiKey);})
                .bodyValue(Map.of("transactionId",t.getTransactionId(),"amount",t.getBillingAmount(),
                        "currency",t.getBillingCurrency(),"country",t.getMerchantCountry(),
                        "destination",c.activeTrip()==null?"":c.activeTrip().getDestinationCountry()))
                .retrieve().bodyToMono(ExternalFraudResult.class).block();
    }
}
