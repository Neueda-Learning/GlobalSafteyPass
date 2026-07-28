package com.travelassistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "readiness_assessments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReadinessAssessment {
    @Id private String id;
    @Column(unique=true, nullable=false) private String tripId;
    private int score;
    @Enumerated(EnumType.STRING) private Enums.ReadinessStatus status;
    @Lob private String checksJson;
    private Instant checkedAt;
}
