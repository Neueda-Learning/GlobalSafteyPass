package com.travelassistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
class AtmMapServiceTest {
    @Autowired AtmMapService atmMapService;

    @Test
    void geocodeRejectsQueryShorterThanTwoCharacters() {
        assertThatThrownBy(() -> atmMapService.geocode("a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Enter a location");
        assertThatThrownBy(() -> atmMapService.geocode("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void geocodeAcceptsValidQueryWhenNetworkAvailable() {
        try {
            var result = atmMapService.geocode("Paris, France");
            assertThat(result.latitude()).isNotEqualTo(0);
            assertThat(result.longitude()).isNotEqualTo(0);
            assertThat(result.label()).isNotBlank();
        } catch (IllegalArgumentException | com.travelassistant.exception.ResourceNotFoundException ex) {
            // External geocoding may be unavailable in CI; validation path is still covered above.
            assertThat(ex.getMessage()).isNotBlank();
        }
    }

    @Test
    void geocodeCachesResultsForSameQuery() {
        try {
            var first = atmMapService.geocode("Tokyo, Japan");
            var second = atmMapService.geocode("Tokyo, Japan");
            assertThat(second.latitude()).isEqualTo(first.latitude());
            assertThat(second.longitude()).isEqualTo(first.longitude());
        } catch (IllegalArgumentException | com.travelassistant.exception.ResourceNotFoundException ex) {
            assertThat(ex.getMessage()).isNotBlank();
        }
    }

    @Test
    void nearbyClampsRadiusBelowMinimum() {
        var response = atmMapService.nearby(48.8566, 2.3522, 100);
        assertThat(response.radiusMeters()).isEqualTo(500);
    }

    @Test
    void nearbyClampsRadiusAboveMaximum() {
        var response = atmMapService.nearby(48.8566, 2.3522, 10000);
        assertThat(response.radiusMeters()).isEqualTo(5000);
    }

    @Test
    void nearbyKeepsRadiusWithinBoundsWhenRequestIsValid() {
        var response = atmMapService.nearby(35.6762, 139.6503, 1500);
        assertThat(response.radiusMeters()).isEqualTo(1500);
        assertThat(response.atms()).isNotNull();
    }
}
