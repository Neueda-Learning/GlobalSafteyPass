package com.travelassistant.analytics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 镜像前端 app.js 中 showTravelAnalytics() 的纯计算逻辑，用于验证 Analytics 图标展示指标。
 */
class TravelAnalyticsLogicTest {

    record TripStub(String id, String destinationCountry, String destinationCity,
            String startDate, String endDate, String status) {}

    static class AnalyticsCalculator {
        static final Map<String, double[]> VISITED_COORDINATES = Map.of(
                "Sydney", new double[]{-33.8688, 151.2093},
                "Tokyo", new double[]{35.6762, 139.6503},
                "Paris", new double[]{48.8566, 2.3522},
                "Japan", new double[]{36.2048, 138.2529},
                "France", new double[]{46.2276, 2.2137}
        );

        static List<TripStub> completedHistory(List<TripStub> trips) {
            return trips.stream().filter(t -> "COMPLETED".equals(t.status())).toList();
        }

        static double worldPercent(int countryCount) {
            return Math.round(countryCount / 195.0 * 1000.0) / 10.0;
        }

        static int uniqueCountries(List<TripStub> completed) {
            return (int) completed.stream().map(TripStub::destinationCountry).distinct().count();
        }

        static List<TripStub> sortByEndDateDesc(List<TripStub> completed) {
            return completed.stream()
                    .sorted(Comparator.comparing(TripStub::endDate).reversed())
                    .toList();
        }

        static List<TripStub> mappableTrips(List<TripStub> completed) {
            return completed.stream()
                    .filter(t -> VISITED_COORDINATES.containsKey(t.destinationCity)
                            || VISITED_COORDINATES.containsKey(t.destinationCountry))
                    .toList();
        }
    }

    private final List<TripStub> sampleTrips = List.of(
            new TripStub("t1", "France", "Paris", "2025-06-01", "2025-06-10", "COMPLETED"),
            new TripStub("t2", "Japan", "Tokyo", "2024-03-01", "2024-03-15", "COMPLETED"),
            new TripStub("t3", "France", "Nice", "2026-08-01", "2026-08-10", "ACTIVE"),
            new TripStub("t4", "Canada", "Toronto", "2026-12-01", "2026-12-10", "PLANNED")
    );

    @Test
    void filtersOnlyCompletedTrips() {
        var history = AnalyticsCalculator.completedHistory(sampleTrips);
        assertThat(history).hasSize(2);
        assertThat(history).allMatch(t -> "COMPLETED".equals(t.status()));
    }

    @Test
    void calculatesWorldPercent() {
        var completed = AnalyticsCalculator.completedHistory(sampleTrips);
        int countries = AnalyticsCalculator.uniqueCountries(completed);
        assertThat(AnalyticsCalculator.worldPercent(countries)).isEqualTo(1.0);
    }

    @Test
    void countsUniqueCountries() {
        var completed = AnalyticsCalculator.completedHistory(sampleTrips);
        assertThat(AnalyticsCalculator.uniqueCountries(completed)).isEqualTo(2);
    }

    @Test
    void sortsHistoryByEndDateDescending() {
        var completed = AnalyticsCalculator.completedHistory(sampleTrips);
        var sorted = AnalyticsCalculator.sortByEndDateDesc(completed);
        assertThat(sorted.get(0).destinationCity()).isEqualTo("Paris");
        assertThat(sorted.get(sorted.size() - 1).destinationCity()).isEqualTo("Tokyo");
    }

    @Test
    void mapsTripsWithKnownCoordinates() {
        var completed = AnalyticsCalculator.completedHistory(sampleTrips);
        var mappable = AnalyticsCalculator.mappableTrips(completed);
        assertThat(mappable).hasSize(2);
        assertThat(mappable).extracting(TripStub::destinationCity).contains("Paris", "Tokyo");
    }

    @Test
    void excludesTripsWithoutCoordinates() {
        var trips = List.of(new TripStub("t5", "Brazil", "Rio", "2023-01-01", "2023-01-10", "COMPLETED"));
        var mappable = AnalyticsCalculator.mappableTrips(AnalyticsCalculator.completedHistory(trips));
        assertThat(mappable).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"0, 0.0", "1, 0.5", "2, 1.0", "10, 5.1"})
    void worldPercentFormula(int countries, double expected) {
        assertThat(AnalyticsCalculator.worldPercent(countries)).isEqualTo(expected);
    }

    @Test
    void emptyHistoryShowsZeroMetrics() {
        var completed = AnalyticsCalculator.completedHistory(List.of());
        assertThat(completed).isEmpty();
        assertThat(AnalyticsCalculator.uniqueCountries(completed)).isZero();
        assertThat(AnalyticsCalculator.worldPercent(0)).isZero();
    }
}
