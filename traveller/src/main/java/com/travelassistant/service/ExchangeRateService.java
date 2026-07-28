package com.travelassistant.service;
import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import com.travelassistant.integration.ExchangeRateProvider;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
@Service
public class ExchangeRateService {
    private static final Logger log=LoggerFactory.getLogger(ExchangeRateService.class);
    private static final Map<String,BigDecimal> USD_REFERENCE=Map.of(
            "USD",BigDecimal.ONE,"JPY",new BigDecimal("149.25"),"EUR",new BigDecimal("0.92"),
            "SGD",new BigDecimal("1.35"),"GBP",new BigDecimal("0.79"),"CNY",new BigDecimal("7.24"));
    private final ExchangeRateProvider provider; private final ConcurrentHashMap<String,ExchangeRateQuote> cache=new ConcurrentHashMap<>();
    public ExchangeRateService(ExchangeRateProvider p){this.provider=p;}
    public ExchangeRateQuote rate(String source,String target,LocalDate date){
        String s=source.toUpperCase(),t=target.toUpperCase(),key="FX:"+s+":"+t+":"+date;
        try{ExchangeRateQuote q=provider.getRate(s,t,date);cache.put(key,q);return q;}
        catch(Exception e){log.warn("FX API unavailable for {}, using fallback: {}",key,e.getMessage());
            ExchangeRateQuote cached=cache.get(key);if(cached!=null)return new ExchangeRateQuote(s,t,cached.rate(),cached.rateDate(),cached.provider()+"-cache",true);
            BigDecimal sourcePerUsd=USD_REFERENCE.get(s),targetPerUsd=USD_REFERENCE.get(t);
            BigDecimal fallback=sourcePerUsd!=null&&targetPerUsd!=null
                    ?targetPerUsd.divide(sourcePerUsd,8,java.math.RoundingMode.HALF_UP):BigDecimal.ONE;
            return new ExchangeRateQuote(s,t,fallback,date,"reference-fallback",true);}
    }
}
