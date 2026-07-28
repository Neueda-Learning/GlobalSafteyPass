package com.travelassistant.service;

import com.travelassistant.integration.ExchangeRateProvider;
import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExchangeRateService")
class ExchangeRateServiceTest {

    @Test @DisplayName("external failure uses reference fallback")
    void externalFailureUsesSafeEstimatedFallback() {
        ExchangeRateProvider failing = (a, b, d) -> { throw new RuntimeException("offline"); };
        var quote = new ExchangeRateService(failing).rate("JPY", "USD", LocalDate.of(2026, 8, 12));
        assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("0.00670017"));
        assertThat(quote.estimated()).isTrue();
        assertThat(quote.provider()).isEqualTo("reference-fallback");
    }

    @Test @DisplayName("same currency from provider")
    void sameCurrencyFromProvider() {
        ExchangeRateProvider provider = (s, t, d) -> new ExchangeRateQuote(s, t, BigDecimal.ONE, d, "Test", false);
        var quote = new ExchangeRateService(provider).rate("USD", "USD", LocalDate.now());
        assertThat(quote.rate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(quote.estimated()).isFalse();
    }

    @Test @DisplayName("uses cache when provider fails after success")
    void usesCacheWhenProviderFailsAfterSuccess() {
        ExchangeRateProvider flaky = new ExchangeRateProvider() {
            private int calls;
            public ExchangeRateQuote getRate(String s, String t, LocalDate d) {
                if (calls++ == 0) return new ExchangeRateQuote(s, t, new BigDecimal("110"), d, "Live", false);
                throw new RuntimeException("down");
            }
        };
        ExchangeRateService svc = new ExchangeRateService(flaky);
        LocalDate date = LocalDate.of(2026, 9, 1);
        assertThat(svc.rate("USD", "JPY", date).provider()).isEqualTo("Live");
        assertThat(svc.rate("USD", "JPY", date).provider()).isEqualTo("Live-cache");
    }

    @Test @DisplayName("EUR to GBP cross-rate fallback")
    void eurToGbpCrossRateFallback() {
        ExchangeRateProvider failing = (a, b, d) -> { throw new RuntimeException("offline"); };
        var quote = new ExchangeRateService(failing).rate("EUR", "GBP", LocalDate.of(2026, 1, 1));
        assertThat(quote.rate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(quote.estimated()).isTrue();
    }

    @Test @DisplayName("unknown pair falls back to 1.0")
    void unknownPairFallsBackToOne() {
        ExchangeRateProvider failing = (a, b, d) -> { throw new RuntimeException("offline"); };
        var quote = new ExchangeRateService(failing).rate("XYZ", "ABC", LocalDate.now());
        assertThat(quote.rate()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
