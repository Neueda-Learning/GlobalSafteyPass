package com.travelassistant.service;

import com.travelassistant.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class AuditServiceIntegrationTest {
    @Autowired AuditService auditService;
    @Autowired AuditLogRepository auditLogs;

    @Test
    void logPersistsAuditRecord() {
        long before = auditLogs.count();
        auditService.log("customer-001", "TEST_ACTION", "TRIP", "trip-test", "details");
        assertThat(auditLogs.count()).isEqualTo(before + 1);
        var latest = auditLogs.findAll().stream()
                .filter(l -> "TEST_ACTION".equals(l.getAction()))
                .findFirst();
        assertThat(latest).isPresent();
        assertThat(latest.get().getCustomerId()).isEqualTo("customer-001");
        assertThat(latest.get().getEntityId()).isEqualTo("trip-test");
    }
}
