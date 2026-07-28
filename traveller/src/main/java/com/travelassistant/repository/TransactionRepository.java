package com.travelassistant.repository;
import com.travelassistant.model.TravelTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface TransactionRepository extends JpaRepository<TravelTransaction,String> {
    List<TravelTransaction> findByTripIdOrderByTransactionTimeDesc(String tripId);
    List<TravelTransaction> findByCardIdAndTransactionTimeBetween(String cardId, Instant from, Instant to);
    List<TravelTransaction> findByCustomerIdOrderByTransactionTimeDesc(String customerId);
}
