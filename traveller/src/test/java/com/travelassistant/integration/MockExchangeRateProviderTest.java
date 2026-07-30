package com.travelassistant.integration;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MockExchangeRateProviderTest {
    private final MockExchangeRateProvider provider = new MockExchangeRateProvider();

    @Test
    void sameCurrencyReturnsOne() {
        var quote = provider.getRate("USD", "USD", LocalDate.now());
        assertThat(quote.rate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(quote.estimated()).isTrue();
    }

    @Test
    void knownPairsReturnFixedRates() {
        assertThat(provider.getRate("JPY", "USD", LocalDate.now()).rate()).isEqualByComparingTo(new BigDecimal("0.0067"));
        assertThat(provider.getRate("EUR", "USD", LocalDate.now()).rate()).isEqualByComparingTo(new BigDecimal("1.09"));
        assertThat(provider.getRate("USD", "JPY", LocalDate.now()).rate()).isEqualByComparingTo(new BigDecimal("149.25"));
    }
}
