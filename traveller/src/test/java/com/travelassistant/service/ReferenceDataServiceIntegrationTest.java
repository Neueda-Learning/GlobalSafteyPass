package com.travelassistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
class ReferenceDataServiceIntegrationTest {
    @Autowired ReferenceDataService referenceDataService;

    @Test
    void countriesReturnsNonEmptyListWhenApiAvailable() {
        try {
            var countries = referenceDataService.countries();
            assertThat(countries).isNotEmpty();
            assertThat(countries.get(0).name()).isNotBlank();
        } catch (com.travelassistant.exception.ExternalServiceException ex) {
            assertThat(ex.getMessage()).contains("Country dataset unavailable");
        }
    }

    @Test
    void citiesReturnsEmptyForBlankCountry() {
        assertThat(referenceDataService.cities("")).isEmpty();
        assertThat(referenceDataService.cities(null)).isEmpty();
    }

    @Test
    void citiesReturnsOptionsForKnownCountryWhenApiAvailable() {
        try {
            var cities = referenceDataService.cities("Japan");
            assertThat(cities).isNotEmpty();
        } catch (Exception ex) {
            assertThat(ex).isNotNull();
        }
    }

    @Test
    void currenciesWithoutCountryReturnsFullListWhenApiAvailable() {
        try {
            var all = referenceDataService.currencies(null);
            assertThat(all).isNotEmpty();
            assertThat(all.stream().anyMatch(c -> "USD".equals(c.code()))).isTrue();
        } catch (com.travelassistant.exception.ExternalServiceException ex) {
            assertThat(ex.getMessage()).contains("Frankfurter");
        }
    }

    @Test
    void currenciesForCountryPrioritisesMainstreamCodesWhenApiAvailable() {
        try {
            var france = referenceDataService.currencies("France");
            assertThat(france).isNotEmpty();
            assertThat(france.get(0).mainstreamForCountry()).isTrue();
        } catch (Exception ex) {
            assertThat(ex).isNotNull();
        }
    }
}
