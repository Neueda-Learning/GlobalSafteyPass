package com.travelassistant.analytics;

import com.travelassistant.TestFixtures;
import com.travelassistant.dto.ApiDtos.TripResponse;
import com.travelassistant.model.Enums.TripStatus;
import com.travelassistant.repository.CardRepository;
import com.travelassistant.repository.TripRepository;
import com.travelassistant.service.TripService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Analytics 图标依赖 GET /api/travel/trips 返回的行程数据。
 * 本测试在 H2 上验证已完成行程的查询与筛选逻辑（Analytics 的数据源）。
 */
@SpringBootTest(properties = "integration.fx.enabled=false")
@ActiveProfiles("test")
@Transactional
class TripAnalyticsIntegrationTest {
    private static final String CUSTOMER = "analytics-test-customer";

    @Autowired TripService tripService;
    @Autowired TripRepository trips;
    @Autowired CardRepository cards;

    @BeforeEach
    void seed() {
        cards.save(TestFixtures.card("card-analytics", CUSTOMER, new BigDecimal("500")));
        trips.save(completedTrip("analytics-trip-paris", "France", "Paris", "2025-06-01", "2025-06-10"));
        trips.save(completedTrip("analytics-trip-tokyo", "Japan", "Tokyo", "2024-03-01", "2024-03-15"));
        trips.save(activeTrip("analytics-trip-nice", "France", "Nice"));
        trips.save(plannedTrip("analytics-trip-future", "Canada", "Toronto"));
    }

    @Test
    void listsAllTripsForCustomer() {
        List<TripResponse> all = tripService.list(CUSTOMER);
        assertThat(all).hasSize(4);
    }

    @Test
    void completedTripsFilterMatchesAnalyticsLogic() {
        List<TripResponse> completed = tripService.list(CUSTOMER).stream()
                .filter(t -> t.status() == TripStatus.COMPLETED).toList();

        assertThat(completed).hasSize(2);
        assertThat(completed).extracting(TripResponse::destinationCountry)
                .containsExactlyInAnyOrder("France", "Japan");
    }

    @Test
    void uniqueCountriesForAnalyticsMetrics() {
        List<TripResponse> completed = tripService.list(CUSTOMER).stream()
                .filter(t -> t.status() == TripStatus.COMPLETED).toList();
        long uniqueCountries = completed.stream().map(TripResponse::destinationCountry).distinct().count();

        assertThat(uniqueCountries).isEqualTo(2);
    }

    @Test
    void excludesNonCompletedTripsFromAnalytics() {
        List<TripResponse> completed = tripService.list(CUSTOMER).stream()
                .filter(t -> t.status() == TripStatus.COMPLETED).toList();

        assertThat(completed).noneMatch(t -> t.destinationCity().equals("Nice"));
        assertThat(completed).noneMatch(t -> t.destinationCity().equals("Toronto"));
    }

    private static com.travelassistant.model.Trip completedTrip(String id, String country, String city,
            String start, String end) {
        var t = TestFixtures.trip(id, CUSTOMER, country, city, TripStatus.COMPLETED);
        t.setStartDate(LocalDate.parse(start));
        t.setEndDate(LocalDate.parse(end));
        return t;
    }

    private static com.travelassistant.model.Trip activeTrip(String id, String country, String city) {
        return TestFixtures.trip(id, CUSTOMER, country, city, TripStatus.ACTIVE);
    }

    private static com.travelassistant.model.Trip plannedTrip(String id, String country, String city) {
        return TestFixtures.trip(id, CUSTOMER, country, city, TripStatus.PLANNED);
    }
}
