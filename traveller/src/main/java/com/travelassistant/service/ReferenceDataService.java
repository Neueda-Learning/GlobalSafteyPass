package com.travelassistant.service;

import com.travelassistant.dto.ApiDtos.CityOption;
import com.travelassistant.dto.ApiDtos.CountryOption;
import com.travelassistant.dto.ApiDtos.CurrencyOption;
import com.travelassistant.exception.ExternalServiceException;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReferenceDataService {
    private static final Duration CACHE_TTL = Duration.ofHours(12);
    private static final String UA = "Mozilla/5.0 (compatible; TravelAssistant/1.0; +https://localhost)";
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("[A-Za-z]{3}");

    private final WebClient countriesClient = WebClient.builder()
        .baseUrl("https://countriesnow.space")
        .defaultHeader("User-Agent", UA)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
        .build();
    private final WebClient frankfurterClient = WebClient.builder()
        .baseUrl("https://api.frankfurter.dev")
        .defaultHeader("User-Agent", UA)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
        .build();

    private volatile List<CountryOption> countriesCache = List.of();
    private volatile Instant countriesCacheAt = Instant.EPOCH;

    private volatile List<CurrencyOption> currenciesCache = List.of();
    private volatile Instant currenciesCacheAt = Instant.EPOCH;

    private final ConcurrentHashMap<String, List<CityOption>> citiesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> citiesCacheAt = new ConcurrentHashMap<>();

    public List<CountryOption> countries() {
        if (!countriesCache.isEmpty() && !isExpired(countriesCacheAt)) {
            return countriesCache;
        }
        synchronized (this) {
            if (!countriesCache.isEmpty() && !isExpired(countriesCacheAt)) {
                return countriesCache;
            }
            CountriesInfoResponse body;
            try {
                body = countriesClient.get().uri("/api/v0.1/countries/info?returns=currency,iso2,iso3")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve().bodyToMono(CountriesInfoResponse.class).block();
            } catch (Exception ex) {
                throw new ExternalServiceException("Country dataset unavailable", ex);
            }
            if (body == null || body.data == null) {
                throw new ExternalServiceException("Country dataset unavailable", null);
            }
            List<CountryOption> options = body.data.stream()
                    .filter(Objects::nonNull)
                    .map(x -> new CountryOption(
                        safeText(x.name),
                            safeUpper(x.iso2),
                            safeUpper(x.iso3),
                            safeUpper(x.currency)
                    ))
                    .filter(x -> !x.name().isBlank())
                    .sorted(Comparator.comparing(CountryOption::name))
                    .toList();
            countriesCache = options;
            countriesCacheAt = Instant.now();
            return options;
        }
    }

    public List<CityOption> cities(String country) {
        String key = country == null ? "" : country.trim();
        if (key.isBlank()) return List.of();
        String cacheKey = key.toUpperCase(Locale.ROOT);
        Instant loadedAt = citiesCacheAt.get(cacheKey);
        List<CityOption> cached = citiesCache.get(cacheKey);
        if (cached != null && loadedAt != null && !isExpired(loadedAt)) {
            return cached;
        }

        Map<String, String> request = Map.of("country", key);
        List<CityOption> options;
        try {
            CitiesResponse body = countriesClient.post().uri("/api/v0.1/countries/cities")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve().bodyToMono(CitiesResponse.class).block();
            if (body == null || Boolean.TRUE.equals(body.error) || body.data == null) {
                throw new ExternalServiceException("City dataset unavailable for " + key, null);
            }
            options = normalizeCities(body.data);
        } catch (Exception ex) {
            options = fallbackCitiesFromFullDataset(key);
        }
        citiesCache.put(cacheKey, options);
        citiesCacheAt.put(cacheKey, Instant.now());
        return options;
    }

    private List<CityOption> fallbackCitiesFromFullDataset(String country) {
        CountriesCitiesResponse body;
        try {
            body = countriesClient.get().uri("/api/v0.1/countries")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(CountriesCitiesResponse.class).block();
        } catch (Exception ex) {
            return defaultCityFallback(country);
        }
        if (body == null || body.data == null) {
            return defaultCityFallback(country);
        }

        String key = country.trim();
        CountryOption matchedCountry = countries().stream()
                .filter(c -> c.name().equalsIgnoreCase(key)
                        || c.iso2().equalsIgnoreCase(key)
                        || c.iso3().equalsIgnoreCase(key))
                .findFirst().orElse(null);
        final String iso2 = matchedCountry == null ? "" : matchedCountry.iso2();
        final String iso3 = matchedCountry == null ? "" : matchedCountry.iso3();

        CountryCities row = body.data.stream()
                .filter(Objects::nonNull)
                .filter(x -> equalsIgnoreCase(x.country, key)
                        || equalsIgnoreCase(x.iso2, key)
                        || equalsIgnoreCase(x.iso3, key)
                        || (!iso2.isBlank() && equalsIgnoreCase(x.iso2, iso2))
                        || (!iso3.isBlank() && equalsIgnoreCase(x.iso3, iso3)))
                .findFirst().orElse(null);

        if (row == null || row.cities == null) {
            return defaultCityFallback(country);
        }

        return normalizeCities(row.cities);
    }

    private List<CityOption> defaultCityFallback(String country) {
        String city = country == null ? "" : country.trim();
        if (city.isBlank()) {
            return List.of();
        }
        return List.of(new CityOption(city));
    }

    private List<CityOption> normalizeCities(List<String> cityNames) {
        if (cityNames == null) {
            return List.of();
        }
        return cityNames.stream().filter(Objects::nonNull)
                .map(String::trim).filter(s -> !s.isBlank())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER)
                .map(CityOption::new).toList();
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    public List<CurrencyOption> currencies(String country) {
        List<CurrencyOption> all = supportedCurrencies();
        if (country == null || country.isBlank()) return all;

        CountryOption match = countries().stream()
                .filter(c -> c.name().equalsIgnoreCase(country)
                        || c.iso2().equalsIgnoreCase(country)
                        || c.iso3().equalsIgnoreCase(country))
                .findFirst().orElse(null);
        List<String> mainstreamCodes = parseMainstreamCurrencies(match == null ? "" : match.mainstreamCurrency());
        if (mainstreamCodes.isEmpty()) return all;
        Map<String, Integer> priority = new LinkedHashMap<>();
        for (int i = 0; i < mainstreamCodes.size(); i++) {
            priority.putIfAbsent(mainstreamCodes.get(i), i);
        }

        Map<String, CurrencyOption> byCode = new LinkedHashMap<>();
        for (CurrencyOption option : all) {
            boolean top = priority.containsKey(option.code().toUpperCase(Locale.ROOT));
            byCode.put(option.code(), new CurrencyOption(option.code(), option.name(), top));
        }

        List<CurrencyOption> ordered = new ArrayList<>();
        for (String code : mainstreamCodes) {
            CurrencyOption top = byCode.remove(code);
            if (top != null) ordered.add(top);
        }
        ordered.addAll(byCode.values());
        return ordered;
    }

    private static List<String> parseMainstreamCurrencies(String mainstreamRaw) {
        if (mainstreamRaw == null || mainstreamRaw.isBlank()) return List.of();
        Matcher matcher = CURRENCY_CODE_PATTERN.matcher(mainstreamRaw.toUpperCase(Locale.ROOT));
        List<String> codes = new ArrayList<>();
        while (matcher.find()) {
            String code = matcher.group();
            if (!codes.contains(code)) codes.add(code);
        }
        return codes;
    }

    private List<CurrencyOption> supportedCurrencies() {
        if (!currenciesCache.isEmpty() && !isExpired(currenciesCacheAt)) {
            return currenciesCache;
        }
        synchronized (this) {
            if (!currenciesCache.isEmpty() && !isExpired(currenciesCacheAt)) {
                return currenciesCache;
            }
            List<FrankfurterCurrency> rows;
            try {
                rows = frankfurterClient.get().uri("/v2/currencies")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve().bodyToFlux(FrankfurterCurrency.class).collectList().block();
            } catch (Exception ex) {
                throw new ExternalServiceException("Frankfurter currency list unavailable", ex);
            }
            if (rows == null || rows.isEmpty()) {
                throw new ExternalServiceException("Frankfurter currency list unavailable", null);
            }
            Map<String, String> unique = new HashMap<>();
            for (FrankfurterCurrency row : rows) {
                if (row == null || row.iso_code == null || row.iso_code.isBlank()) continue;
                String code = row.iso_code.trim().toUpperCase(Locale.ROOT);
                String name = row.name == null || row.name.isBlank() ? code : row.name.trim();
                unique.putIfAbsent(code, name);
            }
            List<CurrencyOption> options = unique.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new CurrencyOption(e.getKey(), e.getValue(), false))
                    .collect(Collectors.toList());
            currenciesCache = options;
            currenciesCacheAt = Instant.now();
            return options;
        }
    }

    private static boolean isExpired(Instant loadedAt) {
        return loadedAt.plus(CACHE_TTL).isBefore(Instant.now());
    }

    private static String safeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static class CountriesInfoResponse {
        public Boolean error;
        public String msg;
        public List<CountryInfo> data;
    }

    private static class CountryInfo {
        public String name;
        public String currency;
        public String iso2;
        public String iso3;
    }

    private static class CitiesResponse {
        public Boolean error;
        public String msg;
        public List<String> data;
    }

    private static class CountriesCitiesResponse {
        public Boolean error;
        public String msg;
        public List<CountryCities> data;
    }

    private static class CountryCities {
        public String country;
        public String iso2;
        public String iso3;
        public List<String> cities;
    }

    private static class FrankfurterCurrency {
        public String iso_code;
        public String name;
    }
}
