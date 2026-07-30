package com.travelassistant.service;

import com.travelassistant.exception.ForbiddenException;
import com.travelassistant.model.Enums;
import com.travelassistant.repository.SupportCaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class SupportCaseServiceIntegrationTest {
    @Autowired SupportCaseService supportCaseService;
    @Autowired SupportCaseRepository supportCaseRepository;

    @Test
    void listReturnsCasesForCustomer() {
        var cases = supportCaseService.list("customer-001");
        assertThat(cases).isNotNull();
    }

    @Test
    void openPaymentCaseCreatesNewCaseWhenNoneExists() {
        long before = supportCaseRepository.count();
        var created = supportCaseService.openPaymentCase("customer-001", "txn-declined");
        assertThat(created.type()).isEqualTo(Enums.CaseType.PAYMENT_SUPPORT);
        assertThat(created.status()).isEqualTo(Enums.CaseStatus.SUBMITTED);
        assertThat(supportCaseRepository.count()).isGreaterThanOrEqualTo(before);
    }

    @Test
    void openPaymentCaseIsIdempotentForSameTransaction() {
        var first = supportCaseService.openPaymentCase("customer-001", "txn-declined");
        var second = supportCaseService.openPaymentCase("customer-001", "txn-declined");
        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void openPaymentCaseRejectsAnotherCustomersTransaction() {
        assertThatThrownBy(() -> supportCaseService.openPaymentCase("customer-002", "txn-declined"))
                .isInstanceOf(ForbiddenException.class);
    }
}
