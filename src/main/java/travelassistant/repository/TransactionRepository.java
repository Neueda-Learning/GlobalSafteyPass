package travelassistant.repository;

import travelassistant.model.Transaction;
import travelassistant.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTripIdOrderByTransactionTimeDesc(Long tripId);
    List<Transaction> findByTripIdAndStatus(Long tripId, TransactionStatus status);
    List<Transaction> findByMerchantAndAmountAndStatus(String merchant, java.math.BigDecimal amount, TransactionStatus status);
}
