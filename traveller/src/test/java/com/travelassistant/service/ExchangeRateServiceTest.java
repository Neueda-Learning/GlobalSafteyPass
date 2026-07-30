package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import com.travelassistant.exception.ExternalServiceException;
import com.travelassistant.integration.ExchangeRateProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateServiceTest {
    private final LocalDate date = LocalDate.of(2026, 8, 12);

    @Test
    void returnsProviderQuoteOnSuccess() {
        ExchangeRateProvider provider = (a, b, d) -> new ExchangeRateQuote("JPY", "USD", new BigDecimal("0.0067"), d, "Mock", false);
        var quote = new ExchangeRateService(provider).rate("JPY", "USD", date);
        assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("0.0067"));
        assertThat(quote.estimated()).isFalse();
    }

    @Test
    void externalFailureUsesCachedQuoteWhenAvailable() {
        ExchangeRateProvider provider = new ExchangeRateProvider() {
            private int calls;
            @Override
            public ExchangeRateQuote getRate(String source, String target, LocalDate d) {
                if (calls++ == 0) return new ExchangeRateQuote(source, target, new BigDecimal("0.0067"), d, "Mock", false);
                throw new RuntimeException("offline");
            }
        };
        ExchangeRateService service = new ExchangeRateService(provider);
        service.rate("JPY", "USD", date);
        var quote = service.rate("JPY", "USD", date);
        assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("0.0067"));
        assertThat(quote.estimated()).isTrue();
        assertThat(quote.provider()).contains("cache");
    }

    @Test
    void externalFailureWithoutCacheThrows() {
        ExchangeRateProvider failing = (a, b, d) -> { throw new RuntimeException("offline"); };
        assertThatThrownBy(() -> new ExchangeRateService(failing).rate("JPY", "USD", date))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("FX rate unavailable");
    }
}
