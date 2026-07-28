package com.globalsafetypass.repository;
import com.globalsafetypass.model.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    List<RiskAlert> findAllByOrderByCreatedAtDesc();
    long countByStatus(RiskAlert.Status status);
}

