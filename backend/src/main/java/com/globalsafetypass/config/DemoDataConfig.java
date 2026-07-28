package com.globalsafetypass.config;

import com.globalsafetypass.api.ApiDtos.BankEvent;
import com.globalsafetypass.model.*;
import com.globalsafetypass.repository.*;
import com.globalsafetypass.service.TransactionIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.time.*;

@Configuration
public class DemoDataConfig {
    @Bean
    CommandLineRunner demoData(UserRepository users, CardRepository cards,
                               TripRepository trips, TransactionIngestionService ingestion) {
        return args -> {
            if (users.count() > 0) return;
            User alex = users.save(new User("Alex Morgan", "alex@example.com"));
            Card primary = cards.save(new Card(alex, "Everyday Travel", "Visa", "4821",
                YearMonth.now().plusYears(3).toString(), new BigDecimal("4200.00"),
                new BigDecimal("1200.00"), new BigDecimal("2000.00"),
                true, false, "USD"));
            cards.save(new Card(alex, "Backup Card", "Mastercard", "1907",
                YearMonth.now().plusMonths(2).toString(), new BigDecimal("850.00"),
                new BigDecimal("300.00"), new BigDecimal("500.00"),
                false, false, "USD"));
            LocalDate today = LocalDate.now();
            trips.save(new Trip(alex, primary, "Singapore", "Singapore",
                today.minusDays(2), today.plusDays(5), new BigDecimal("2500.00"), "USD"));

            ingestion.ingest(event(primary, "Harbour Coffee", "18.40", "Singapore",
                "Singapore", "Dining", "CONTACTLESS", LocalDateTime.now().minusHours(5), "seed-1"));
            ingestion.ingest(event(primary, "Marina Transit", "7.80", "Singapore",
                "Singapore", "Transport", "CONTACTLESS", LocalDateTime.now().minusHours(3), "seed-2"));
            ingestion.ingest(event(primary, "Luxury Gallery", "820.00", "Japan",
                "Tokyo", "Shopping", "ONLINE", LocalDateTime.now().minusHours(1), "seed-3"));
        };
    }

    private BankEvent event(Card card, String merchant, String amount, String country,
                            String city, String category, String channel,
                            LocalDateTime occurredAt, String id) {
        return new BankEvent("DEMO_BANK", id, "event-" + id, card.getId(), merchant,
            new BigDecimal(amount), "USD", BigDecimal.ONE, country, city,
            category, channel, occurredAt);
    }
}

