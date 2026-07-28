package travelassistant.config;

import travelassistant.model.Card;
import travelassistant.repository.CardRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(CardRepository cardRepository) {
        return args -> {
            if (cardRepository.count() > 0) return;

            Card card1 = new Card();
            card1.setCardName("Global Travel Card");
            card1.setCardNumber("4532123456789012");
            card1.setExpiryDate(LocalDate.of(2027, 12, 31));
            card1.setOverseasEnabled(true);
            card1.setBalance(new BigDecimal("50000.00"));
            card1.setDailyLimit(new BigDecimal("20000.00"));
            card1.setCurrency("CNY");
            cardRepository.save(card1);

            Card card2 = new Card();
            card2.setCardName("Debit Card");
            card2.setCardNumber("6217001234567890");
            card2.setExpiryDate(LocalDate.of(2025, 6, 30));
            card2.setOverseasEnabled(false);
            card2.setBalance(new BigDecimal("3000.00"));
            card2.setDailyLimit(new BigDecimal("5000.00"));
            card2.setCurrency("CNY");
            cardRepository.save(card2);

            Card card3 = new Card();
            card3.setCardName("Business Platinum Card");
            card3.setCardNumber("4111111111111111");
            card3.setExpiryDate(LocalDate.of(2028, 3, 15));
            card3.setOverseasEnabled(true);
            card3.setBalance(new BigDecimal("150000.00"));
            card3.setDailyLimit(new BigDecimal("50000.00"));
            card3.setFrozen(true);
            card3.setCurrency("CNY");
            cardRepository.save(card3);
        };
    }
}
