package com.travelassistant.integration;

import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
@ConditionalOnProperty(name="integration.fx.enabled", havingValue="true", matchIfMissing=true)
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {
    private final WebClient client;
    public FrankfurterExchangeRateProvider(WebClient fxWebClient) { this.client = fxWebClient; }
    @Override public ExchangeRateQuote getRate(String source, String target, LocalDate date) {
        if (source.equalsIgnoreCase(target)) return new ExchangeRateQuote(source, target, BigDecimal.ONE, date, "identity", false);
        FxResponse body = client.get().uri(uriBuilder->uriBuilder.path("/latest")
                        .queryParam("base",source).queryParam("symbols",target).build())
                .retrieve().bodyToMono(FxResponse.class).block();
        if (body == null || body.rates() == null || body.rates().get(target) == null) throw new IllegalStateException("FX rate unavailable");
        return new ExchangeRateQuote(source, target, body.rates().get(target), body.date(), "Frankfurter", false);
    }
    private record FxResponse(BigDecimal amount, String base, LocalDate date, Map<String,BigDecimal> rates) {}
}
