package com.travelassistant.integration;

import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import com.travelassistant.exception.ExternalServiceException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name="integration.fx.enabled", havingValue="true", matchIfMissing=true)
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {
    private final WebClient client;
    public FrankfurterExchangeRateProvider(WebClient fxWebClient) { this.client = fxWebClient; }
    @Override public ExchangeRateQuote getRate(String source, String target, LocalDate date) {
        if (source.equalsIgnoreCase(target)) return new ExchangeRateQuote(source, target, BigDecimal.ONE, date, "identity", false);
        try {
            List<FxRateRow> rows = client.get().uri(uriBuilder -> uriBuilder.path("/v2/rates")
                            .queryParam("base", source).queryParam("quotes", target).build())
                    .retrieve().bodyToFlux(FxRateRow.class).collectList().block();
            if (rows == null || rows.isEmpty()) {
                throw new ExternalServiceException("Frankfurter returned no rate for " + source + "->" + target, null);
            }
            FxRateRow row = rows.stream().filter(r -> r.quote() != null && target.equalsIgnoreCase(r.quote())).findFirst().orElse(rows.get(0));
            if (row.rate() == null) {
                throw new ExternalServiceException("Frankfurter returned null rate for " + source + "->" + target, null);
            }
            LocalDate rateDate = row.date() == null ? date : row.date();
            return new ExchangeRateQuote(source, target, row.rate(), rateDate, "Frankfurter", false);
        } catch (WebClientResponseException ex) {
            throw new ExternalServiceException("Frankfurter API request failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExternalServiceException("Frankfurter API request failed", ex);
        }
    }
    private record FxRateRow(LocalDate date, String base, String quote, BigDecimal rate) {}
}
