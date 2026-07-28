package com.travelassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.exception.ResourceNotFoundException;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.IOException;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AtmMapServiceTest {
    private MockWebServer geocodingServer;
    private MockWebServer placesServer;
    private AtmMapService service;

    @BeforeEach
    void setUp() throws IOException {
        geocodingServer = new MockWebServer();
        placesServer = new MockWebServer();
        geocodingServer.start();
        placesServer.start();
        WebClient geocoding = WebClient.builder().baseUrl(geocodingServer.url("/").toString()).build();
        WebClient places = WebClient.builder().baseUrl(placesServer.url("/").toString()).build();
        service = new AtmMapService(geocoding, places);
    }

    @AfterEach
    void tearDown() throws IOException {
        geocodingServer.shutdown();
        placesServer.shutdown();
    }

    @Test
    void geocodeReturnsLocationFromNominatim() throws Exception {
        String body = new ObjectMapper().writeValueAsString(new Object[]{
                Map.of("display_name", "Paris, France", "lat", "48.8566", "lon", "2.3522")
        });
        geocodingServer.enqueue(new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));

        MapLocationResponse result = service.geocode("Paris");

        assertThat(result.query()).isEqualTo("Paris");
        assertThat(result.label()).contains("Paris");
        assertThat(result.latitude()).isEqualTo(48.8566);
        assertThat(result.longitude()).isEqualTo(2.3522);
        assertThat(geocodingServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void geocodeCachesRepeatedQueries() throws Exception {
        String body = "[{\"display_name\":\"Tokyo\",\"lat\":\"35.6762\",\"lon\":\"139.6503\"}]";
        geocodingServer.enqueue(new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));

        service.geocode("Tokyo");
        service.geocode("tokyo");

        assertThat(geocodingServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void geocodeRejectsShortQuery() {
        assertThatThrownBy(() -> service.geocode("a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Enter a location");
    }

    @Test
    void geocodeThrowsWhenLocationNotFound() {
        geocodingServer.enqueue(new MockResponse().setBody("[]").addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> service.geocode("NowhereLand"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Location not found");
    }

    @Test
    void nearbyClampsRadiusToValidRange() throws Exception {
        String overpassBody = "{\"elements\":[]}";
        String nominatimBody = "[{\"category\":\"amenity\",\"type\":\"atm\",\"name\":\"City ATM\","
                + "\"lat\":\"48.857\",\"lon\":\"2.353\",\"osm_type\":\"node\",\"osm_id\":\"1\","
                + "\"address\":{\"road\":\"Rue Test\",\"city\":\"Paris\"}}]";
        placesServer.enqueue(new MockResponse().setBody(overpassBody).addHeader("Content-Type", "application/json"));
        geocodingServer.enqueue(new MockResponse().setBody(nominatimBody).addHeader("Content-Type", "application/json"));

        AtmSearchResponse result = service.nearby(48.8566, 2.3522, 100);

        assertThat(result.radiusMeters()).isEqualTo(500);
        assertThat(result.atms()).isNotEmpty();
    }

    @Test
    void nearbyReturnsAtmsFromOverpass() throws Exception {
        String overpassBody = """
                {"elements":[{"type":"node","id":42,"lat":48.857,"lon":2.353,
                "tags":{"brand":"HSBC","operator":"HSBC","addr:street":"Rue Test","addr:city":"Paris"}}]}
                """;
        placesServer.enqueue(new MockResponse().setBody(overpassBody).addHeader("Content-Type", "application/json"));

        AtmSearchResponse result = service.nearby(48.8566, 2.3522, 2000);

        assertThat(result.provider()).isEqualTo("OpenStreetMap");
        assertThat(result.atms()).hasSize(1);
        assertThat(result.atms().get(0).name()).isEqualTo("HSBC");
        assertThat(result.atms().get(0).distanceMeters()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void nearbyFallsBackToNominatimWhenOverpassFails() {
        placesServer.enqueue(new MockResponse().setResponseCode(500));
        String nominatimBody = "[{\"category\":\"amenity\",\"type\":\"atm\",\"name\":\"Backup ATM\","
                + "\"lat\":\"48.857\",\"lon\":\"2.353\",\"osm_type\":\"node\",\"osm_id\":\"9\","
                + "\"display_name\":\"Backup ATM, Paris\"}]";
        geocodingServer.enqueue(new MockResponse().setBody(nominatimBody).addHeader("Content-Type", "application/json"));

        AtmSearchResponse result = service.nearby(48.8566, 2.3522, 2000);

        assertThat(result.atms()).hasSize(1);
        assertThat(result.atms().get(0).name()).isEqualTo("Backup ATM");
    }

    @Test
    void nearbyReturnsEmptyWhenAllProvidersFail() {
        placesServer.enqueue(new MockResponse().setResponseCode(503));
        geocodingServer.enqueue(new MockResponse().setResponseCode(503));

        AtmSearchResponse result = service.nearby(48.8566, 2.3522, 2000);

        assertThat(result.atms()).isEmpty();
        assertThat(result.provider()).contains("unavailable");
    }

    @Test
    void nearbyCachesResultsForSameCoordinates() throws Exception {
        String overpassBody = "{\"elements\":[{\"type\":\"node\",\"id\":1,\"lat\":35.68,\"lon\":139.65,\"tags\":{\"name\":\"ATM\"}}]}";
        placesServer.enqueue(new MockResponse().setBody(overpassBody).addHeader("Content-Type", "application/json"));

        service.nearby(35.6762, 139.6503, 2000);
        service.nearby(35.6762, 139.6503, 2000);

        assertThat(placesServer.getRequestCount()).isEqualTo(1);
    }
}
