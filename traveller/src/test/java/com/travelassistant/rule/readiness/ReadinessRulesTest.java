package com.travelassistant.rule.readiness;

import com.travelassistant.model.Enums;
import com.travelassistant.service.CardCapabilityService;
import com.travelassistant.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadinessRulesTest {
    @Mock private com.travelassistant.repository.CardRepository cardRepository;

    private CardExpiryRule cardExpiry;
    private CardFrozenRule cardFrozen;
    private OverseasPaymentRule overseasPayment;
    private OnlinePaymentRule onlinePayment;
    private DailyPaymentLimitRule paymentLimit;
    private WithdrawalLimitRule withdrawalLimit;
    private AvailableBalanceRule availableBalance;
    private AccountStatusRule accountStatus;
    private CurrencySupportRule currencySupport;
    private BackupCardRule backupCard;

    @BeforeEach
    void setUp() {
        cardExpiry = new CardExpiryRule();
        cardFrozen = new CardFrozenRule();
        overseasPayment = new OverseasPaymentRule();
        onlinePayment = new OnlinePaymentRule();
        paymentLimit = new DailyPaymentLimitRule();
        withdrawalLimit = new WithdrawalLimitRule();
        availableBalance = new AvailableBalanceRule();
        accountStatus = new AccountStatusRule();
        currencySupport = new CurrencySupportRule(new CardCapabilityService());
        backupCard = new BackupCardRule(cardRepository);
    }

    @Test
    void cardExpiryFailsWhenCardExpiresBeforeTripEnd() {
        var trip = trip(LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 12));
        var card = TestDataFactory.card("card-exp", "c1", Enums.CardStatus.ACTIVE);
        card.setExpiryMonth(8);
        card.setExpiryYear(2026);
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        assertThat(cardExpiry.evaluate(trip, card, account).passed()).isFalse();
    }

    @Test
    void cardFrozenRuleFailsForFrozenCard() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        var card = TestDataFactory.card("card-frozen", "c1", Enums.CardStatus.FROZEN);
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        assertThat(cardFrozen.evaluate(trip, card, account).passed()).isFalse();
    }

    @Test
    void overseasPaymentRuleFailsWhenDisabled() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        card.setOverseasPaymentsEnabled(false);
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        assertThat(overseasPayment.evaluate(trip, card, account).passed()).isFalse();
    }

    @Test
    void onlinePaymentRuleFailsWhenDisabled() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        card.setOnlinePaymentsEnabled(false);
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        assertThat(onlinePayment.evaluate(trip, card, account).passed()).isFalse();
    }

    @Test
    void paymentLimitFailsWhenDailyLimitTooLow() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        trip.setBudget(new BigDecimal("3200"));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        card.setDailyPaymentLimit(new BigDecimal("100"));
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        assertThat(paymentLimit.evaluate(trip, card, account).passed()).isFalse();
    }

    @Test
    void withdrawalLimitFailsWhenBelowMinimum() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        card.setDailyWithdrawalLimit(new BigDecimal("50"));
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        assertThat(withdrawalLimit.evaluate(trip, card, account).passed()).isFalse();
    }

    @Test
    void availableBalanceFailsWhenBelowBudget() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        trip.setBudget(new BigDecimal("10000"));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", new BigDecimal("500"), Enums.AccountStatus.ACTIVE);
        assertThat(availableBalance.evaluate(trip, card, account).passed()).isFalse();
    }

    @Test
    void accountStatusFailsWhenNotActive() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.FROZEN);
        assertThat(accountStatus.evaluate(trip, card, account).passed()).isFalse();
    }

    @Test
    void currencySupportPassesForUsdSettlementCard() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        trip.setDestinationCountry("Japan");
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        card.setMainCurrency("USD");
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        assertThat(currencySupport.evaluate(trip, card, account).passed()).isTrue();
    }

    @Test
    void currencySupportAcceptsUsdSettlementFallback() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        trip.setDestinationCountry("Japan");
        trip.setCashExchangeMethod("USD_SETTLEMENT");
        var card = TestDataFactory.card("card-001", "c1", Enums.CardStatus.ACTIVE);
        card.setMainCurrency("GBP");
        card.setSupportedCurrencies("GBP");
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        assertThat(currencySupport.evaluate(trip, card, account).passed()).isTrue();
    }

    @Test
    void backupCardRuleFailsWhenNoOtherActiveCard() {
        var trip = trip(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        var card = TestDataFactory.card("card-only", "c1", Enums.CardStatus.ACTIVE);
        var account = TestDataFactory.account("a1", new BigDecimal("5000"), Enums.AccountStatus.ACTIVE);
        when(cardRepository.findByCustomerId("c1")).thenReturn(java.util.List.of(card));
        assertThat(backupCard.evaluate(trip, card, account).passed()).isFalse();
    }

    private static com.travelassistant.model.Trip trip(LocalDate start, LocalDate end) {
        return TestDataFactory.trip("trip-test", "c1", "France", start, end);
    }
}
