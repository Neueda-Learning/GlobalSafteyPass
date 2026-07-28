package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="support_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportCase {
    @Id private String id;
    @Column(nullable=false) private String customerId;
    private String tripId;
    private String transactionId;
    @Enumerated(EnumType.STRING) private Enums.CaseType type;
    @Enumerated(EnumType.STRING) private Enums.CaseStatus status;
    private String title;
    @Column(length=1000) private String currentUpdate;
    private Instant createdAt;
    private Instant updatedAt;
}
