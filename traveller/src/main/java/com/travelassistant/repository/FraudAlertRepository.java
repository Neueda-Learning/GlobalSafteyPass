package com.travelassistant.repository;
import com.travelassistant.model.FraudAlert;
import com.travelassistant.model.Enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FraudAlertRepository extends JpaRepository<FraudAlert,String> {
    List<FraudAlert> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    long countByTripIdAndStatus(String tripId, AlertStatus status);
    List<FraudAlert> findByTransactionId(String transactionId);
}
