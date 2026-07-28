package com.travelassistant.repository;

import com.travelassistant.TestFixtures;
import com.travelassistant.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Repository H2 Integration")
class RepositoryIntegrationTest {
    @Autowired TripRepository trips;
    @Autowired CardRepository cards;
    @Autowired AccountRepository accounts;
    @Autowired TransactionRepository transactions;
    @Autowired FraudAlertRepository alerts;

    @Test @DisplayName("persists and finds trip")
    void persistsAndFindsTrip() {
        Trip trip = TestFixtures.trip("trip-h2-1", "customer-001");
        trips.save(trip);
        assertThat(trips.findById("trip-h2-1")).isPresent();
        assertThat(trips.findByCustomerIdOrderByStartDateDesc("customer-001")).hasSize(1);
    }

    @Test @DisplayName("persists card and account")
    void persistsCardAndAccount() {
        accounts.save(TestFixtures.activeAccount("acct-h2-1", "customer-001"));
        cards.save(TestFixtures.activeCard("card-h2-1", "customer-001"));
        assertThat(cards.findByCustomerId("customer-001")).hasSize(1);
    }

    @Test @DisplayName("persists transaction")
    void persistsTransaction() {
        transactions.save(TestFixtures.purchase("txn-h2-1", "customer-001", "trip-1"));
        assertThat(transactions.findByCustomerIdOrderByTransactionTimeDesc("customer-001")).hasSize(1);
    }

    @Test @DisplayName("persists fraud alert")
    void persistsFraudAlert() {
        alerts.save(TestFixtures.openAlert("alert-h2-1", "customer-001", "txn-1"));
        assertThat(alerts.findByCustomerIdOrderByCreatedAtDesc("customer-001")).hasSize(1);
    }
}
