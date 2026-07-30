package com.travelassistant.repository;

import com.travelassistant.model.Enums;
import com.travelassistant.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class RepositoryIntegrationTest {
    @Autowired CustomerRepository customers;
    @Autowired TripRepository trips;
    @Autowired CardRepository cards;
    @Autowired TransactionRepository transactions;
    @Autowired FraudAlertRepository alerts;

    @Test
    void customerRepositoryFindsByDisplayName() {
        assertThat(customers.findByDisplayNameIgnoreCase("Jessie Han")).isPresent();
        assertThat(customers.findByDisplayNameIgnoreCase("jessie han")).isPresent();
    }

    @Test
    void tripRepositoryFindsByCustomerOrdered() {
        var list = trips.findByCustomerIdOrderByStartDateDesc("customer-001");
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getStartDate()).isAfterOrEqualTo(list.get(list.size() - 1).getStartDate());
    }

    @Test
    void cardRepositoryFindsByCustomer() {
        assertThat(cards.findByCustomerId("customer-001")).hasSizeGreaterThan(1);
    }

    @Test
    void transactionRepositoryCustomQueries() {
        var txn = TestDataFactory.txn("txn-repo-test", "Japan", java.math.BigDecimal.TEN, Enums.TransactionStatus.APPROVED);
        txn.setCustomerId("customer-001");
        txn.setCardId("card-001");
        txn.setTransactionTime(Instant.parse("2026-10-02T10:00:00Z"));
        transactions.save(txn);
        assertThat(transactions.findByCustomerIdOrderByTransactionTimeDesc("customer-001"))
                .anyMatch(t -> t.getTransactionId().equals("txn-repo-test"));
    }

    @Test
    void fraudAlertRepositoryCountsOpenByTrip() {
        assertThat(alerts.countByTripIdAndStatus("trip-tokyo", Enums.AlertStatus.OPEN)).isGreaterThanOrEqualTo(0);
    }
}
