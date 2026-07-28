package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id private String id;
    private String customerId;
    private String action;
    private String entityType;
    private String entityId;
    @Column(length=2000) private String details;
    private Instant timestamp;
}
