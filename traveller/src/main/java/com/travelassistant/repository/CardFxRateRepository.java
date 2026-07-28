package com.travelassistant.repository;
import com.travelassistant.model.CardFxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CardFxRateRepository extends JpaRepository<CardFxRate,String> {
    Optional<CardFxRate> findByCardIdAndTargetCurrency(String cardId,String targetCurrency);
}
