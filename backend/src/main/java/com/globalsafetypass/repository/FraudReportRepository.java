package com.globalsafetypass.repository;
import com.globalsafetypass.model.FraudReport;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FraudReportRepository extends JpaRepository<FraudReport, Long> {}

