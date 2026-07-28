package com.travelassistant.service;

import com.travelassistant.TestFixtures;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.integration.LocalCardSystemClient;
import com.travelassistant.model.Card;
import com.travelassistant.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "integration.fx.enabled=false")
@ActiveProfiles("test")
@Transactional
class CardControlServiceAtmIntegrationTest {
    @Autowired CardControlService cardControl;
    @Autowired CardRepository cards;
    @Autowired AuditLogRepository auditLogs;

    @BeforeEach
    void seedCard() {
        cards.save(TestFixtures.card("card-atm-test", "customer-001", new BigDecimal("200")));
    }

    @Test
    void updatesWithdrawalLimit() {
        CardResponse response = cardControl.withdrawalLimit("customer-001", "card-atm-test", new BigDecimal("1000"));

        assertThat(response.dailyWithdrawalLimit()).isEqualByComparingTo("1000");
        Card persisted = cards.findById("card-atm-test").orElseThrow();
        assertThat(persisted.getDailyWithdrawalLimit()).isEqualByComparingTo("1000");
    }

    @Test
    void auditLogRecordedForWithdrawalLimitChange() {
        cardControl.withdrawalLimit("customer-001", "card-atm-test", new BigDecimal("800"));

        assertThat(auditLogs.findAll()).anyMatch(log ->
                "CARD_LIMIT_CHANGED".equals(log.getAction()) && log.getDetails().contains("withdrawal=800"));
    }
}
