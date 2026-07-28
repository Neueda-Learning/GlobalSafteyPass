package com.travelassistant.service;
import com.travelassistant.model.AuditLog;
import com.travelassistant.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
@Service
public class AuditService {
    private final AuditLogRepository repo;
    public AuditService(AuditLogRepository repo){this.repo=repo;}
    public void log(String customer,String action,String type,String id,String details){
        repo.save(AuditLog.builder().id(UUID.randomUUID().toString()).customerId(customer).action(action)
                .entityType(type).entityId(id).details(details).timestamp(Instant.now()).build());
    }
}
