package com.travelassistant.service;
import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import com.travelassistant.integration.ExchangeRateProvider;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.assertThat;
class ExchangeRateServiceTest {
    @Test void externalFailureUsesCachedQuote(){
        AtomicBoolean firstCall=new AtomicBoolean(true);
        ExchangeRateProvider flaky=(a,b,d)->{
            if(firstCall.getAndSet(false))return new ExchangeRateQuote(a,b,new BigDecimal("149.25000000"),d,"Frankfurter",false);
            throw new RuntimeException("offline");
        };
        ExchangeRateService service=new ExchangeRateService(flaky);
        var live=service.rate("USD","JPY", LocalDate.of(2026,8,12));
        var cached=service.rate("USD","JPY", LocalDate.of(2026,8,12));
        assertThat(cached.rate()).isEqualByComparingTo(live.rate());
        assertThat(cached.estimated()).isTrue();
        assertThat(cached.provider()).isEqualTo("Frankfurter-cache");
    }
}
