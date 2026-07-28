package com.travelassistant.repository;
import com.travelassistant.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog,String> {}
