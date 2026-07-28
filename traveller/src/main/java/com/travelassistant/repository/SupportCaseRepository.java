package com.travelassistant.repository;

import com.travelassistant.model.SupportCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SupportCaseRepository extends JpaRepository<SupportCase,String> {
    List<SupportCase> findByCustomerIdOrderByUpdatedAtDesc(String customerId);
    Optional<SupportCase> findFirstByCustomerIdAndTransactionIdOrderByCreatedAtDesc(String customerId,String transactionId);
}
