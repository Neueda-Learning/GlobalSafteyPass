package com.travelassistant.integration;
import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
@Component
@ConditionalOnProperty(name="integration.fx.enabled", havingValue="false")
public class MockExchangeRateProvider implements ExchangeRateProvider {
    public ExchangeRateQuote getRate(String source, String target, LocalDate date) {
        BigDecimal rate = source.equals(target) ? BigDecimal.ONE : switch (source + target) {
            case "JPYUSD" -> new BigDecimal("0.0067");
            case "EURUSD" -> new BigDecimal("1.09");
            case "USDJPY" -> new BigDecimal("149.25");
            case "USDCAD" -> new BigDecimal("1.38");
            default -> BigDecimal.ONE;
        };
        return new ExchangeRateQuote(source,target,rate,date,"Mock",true);
    }
}
