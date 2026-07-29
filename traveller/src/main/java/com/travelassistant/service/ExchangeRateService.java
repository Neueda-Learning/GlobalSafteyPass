package com.travelassistant.service;
import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import com.travelassistant.exception.ExternalServiceException;
import com.travelassistant.integration.ExchangeRateProvider;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class ExchangeRateService {
    private static final Logger log=LoggerFactory.getLogger(ExchangeRateService.class);
    private final ExchangeRateProvider provider; private final ConcurrentHashMap<String,ExchangeRateQuote> cache=new ConcurrentHashMap<>();
    public ExchangeRateService(ExchangeRateProvider p){this.provider=p;}
    public ExchangeRateQuote rate(String source,String target,LocalDate date){
        String s=source.toUpperCase(),t=target.toUpperCase(),key="FX:"+s+":"+t+":"+date;
        try{ExchangeRateQuote q=provider.getRate(s,t,date);cache.put(key,q);return q;}
        catch(Exception e){log.warn("FX API unavailable for {}, using fallback: {}",key,e.getMessage());
            ExchangeRateQuote cached=cache.get(key);if(cached!=null)return new ExchangeRateQuote(s,t,cached.rate(),cached.rateDate(),cached.provider()+"-cache",true);
            throw new ExternalServiceException("FX rate unavailable and no cached quote for "+s+"->"+t+" on "+date,e);}
    }
}
