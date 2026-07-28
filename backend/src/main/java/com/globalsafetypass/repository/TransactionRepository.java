package com.globalsafetypass.repository;
import com.globalsafetypass.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {
    boolean existsByProviderAndExternalTransactionId(String provider, String externalTransactionId);
    boolean existsByEventId(String eventId);
    List<BankTransaction> findTop50ByOrderByOccurredAtDesc();
    List<BankTransaction> findByCardIdAndStatusAndOccurredAtBetween(
        Long cardId, BankTransaction.Status status, LocalDateTime from, LocalDateTime to);
    List<BankTransaction> findByCardIdAndMerchantIgnoreCaseAndAmountAndOccurredAtBetween(
        Long cardId, String merchant, BigDecimal amount, LocalDateTime from, LocalDateTime to);
    List<BankTransaction> findByCardIdAndChannelIgnoreCaseAndOccurredAtBetween(
        Long cardId, String channel, LocalDateTime from, LocalDateTime to);
}

