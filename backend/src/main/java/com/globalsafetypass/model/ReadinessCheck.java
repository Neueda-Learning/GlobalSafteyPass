package com.globalsafetypass.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "readiness_checks")
public class ReadinessCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Trip trip;
    @Column(nullable = false) private int score;
    @Column(nullable = false) private String status;
    @Lob @Column(nullable = false) private String resultJson;
    @Column(nullable = false) private LocalDateTime checkedAt = LocalDateTime.now();

    public ReadinessCheck() {}
    public ReadinessCheck(Trip trip, int score, String status, String resultJson) {
        this.trip = trip; this.score = score; this.status = status; this.resultJson = resultJson;
    }
}

