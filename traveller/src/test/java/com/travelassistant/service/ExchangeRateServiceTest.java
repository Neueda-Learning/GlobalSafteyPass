package com.travelassistant.service;
import com.travelassistant.integration.ExchangeRateProvider;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
class ExchangeRateServiceTest {
    @Test void externalFailureUsesSafeEstimatedFallback(){
        ExchangeRateProvider failing=(a,b,d)->{throw new RuntimeException("offline");};
        var quote=new ExchangeRateService(failing).rate("JPY","USD", LocalDate.of(2026,8,12));
        assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("0.00670017"));
        assertThat(quote.estimated()).isTrue();
        assertThat(quote.provider()).isEqualTo("reference-fallback");
    }
}
