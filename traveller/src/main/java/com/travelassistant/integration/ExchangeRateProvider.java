package com.travelassistant.integration;
import com.travelassistant.dto.ApiDtos.ExchangeRateQuote;
import java.time.LocalDate;
public interface ExchangeRateProvider { ExchangeRateQuote getRate(String sourceCurrency, String targetCurrency, LocalDate date); }
