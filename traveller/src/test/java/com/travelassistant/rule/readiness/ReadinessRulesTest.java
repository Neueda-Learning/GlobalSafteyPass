package com.travelassistant.rule.readiness;

import com.travelassistant.TestFixtures;
import com.travelassistant.model.*;
import com.travelassistant.repository.CardRepository;
import com.travelassistant.service.CardCapabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.time.YearMonth;
import static com.travelassistant.model.Enums.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Readiness Rules")
class ReadinessRulesTest {
    private Trip trip;
    private Card card;
    private Account account;

    @BeforeEach
    void setUp() {
        trip = TestFixtures.trip("trip-1", "customer-001");
        card = TestFixtures.activeCard("card-001", "customer-001");
        account = TestFixtures.activeAccount("acct-001", "customer-001");
    }

    @Test @DisplayName("card expiry valid")
    void cardExpiryValid() {
        assertThat(new CardExpiryRule().evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("card expired before trip end")
    void cardExpiredBeforeTripEnd() {
        card.setExpiryYear(YearMonth.now().getYear() - 1);
        assertThat(new CardExpiryRule().evaluate(trip, card, account).passed()).isFalse();
    }

    @Test @DisplayName("active card passes status rule")
    void activeCardPassesStatusRule() {
        assertThat(new CardFrozenRule().evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("frozen card fails status rule")
    void frozenCardFailsStatusRule() {
        card.setStatus(CardStatus.FROZEN);
        assertThat(new CardFrozenRule().evaluate(trip, card, account).passed()).isFalse();
    }

    @Test @DisplayName("overseas payments enabled")
    void overseasPaymentsEnabled() {
        assertThat(new OverseasPaymentRule().evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("overseas payments disabled")
    void overseasPaymentsDisabled() {
        card.setOverseasPaymentsEnabled(false);
        assertThat(new OverseasPaymentRule().evaluate(trip, card, account).passed()).isFalse();
    }

    @Test @DisplayName("online payments enabled")
    void onlinePaymentsEnabled() {
        assertThat(new OnlinePaymentRule().evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("sufficient daily payment limit")
    void sufficientDailyPaymentLimit() {
        card.setDailyPaymentLimit(new BigDecimal("5000"));
        assertThat(new DailyPaymentLimitRule().evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("low daily payment limit")
    void lowDailyPaymentLimit() {
        card.setDailyPaymentLimit(new BigDecimal("10"));
        assertThat(new DailyPaymentLimitRule().evaluate(trip, card, account).passed()).isFalse();
    }

    @Test @DisplayName("sufficient balance")
    void sufficientBalance() {
        assertThat(new AvailableBalanceRule().evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("insufficient balance")
    void insufficientBalance() {
        account.setAvailableBalance(new BigDecimal("100"));
        assertThat(new AvailableBalanceRule().evaluate(trip, card, account).passed()).isFalse();
    }

    @Test @DisplayName("active account passes")
    void activeAccountPasses() {
        assertThat(new AccountStatusRule().evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("frozen account fails")
    void frozenAccountFails() {
        account.setStatus(AccountStatus.FROZEN);
        assertThat(new AccountStatusRule().evaluate(trip, card, account).passed()).isFalse();
    }

    @Test @DisplayName("currency support for Japan JPY")
    void currencySupportForJapan() {
        var caps = new CardCapabilityService();
        assertThat(new CurrencySupportRule(caps).evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("backup card available")
    void backupCardAvailable() {
        CardRepository repo = mock(CardRepository.class);
        Card backup = TestFixtures.activeCard("card-002", "customer-001");
        when(repo.findByCustomerId("customer-001")).thenReturn(java.util.List.of(card, backup));
        assertThat(new BackupCardRule(repo).evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("no backup card")
    void noBackupCard() {
        CardRepository repo = mock(CardRepository.class);
        when(repo.findByCustomerId("customer-001")).thenReturn(java.util.List.of(card));
        assertThat(new BackupCardRule(repo).evaluate(trip, card, account).passed()).isFalse();
    }

    @Test @DisplayName("withdrawal limit sufficient")
    void withdrawalLimitSufficient() {
        assertThat(new WithdrawalLimitRule().evaluate(trip, card, account).passed()).isTrue();
    }

    @Test @DisplayName("withdrawal limit too low")
    void withdrawalLimitTooLow() {
        card.setDailyWithdrawalLimit(new BigDecimal("50"));
        assertThat(new WithdrawalLimitRule().evaluate(trip, card, account).passed()).isFalse();
    }
}
