package com.travelassistant.config;

import com.travelassistant.model.*;
import com.travelassistant.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Configuration
public class DataSeeder {
    @Bean CommandLineRunner seed(CardRepository cards,AccountRepository accounts,TripRepository trips,
            TransactionRepository txs,FraudAlertRepository alerts,SupportCaseRepository cases){
        return args->{
            if(cards.count()>0){
                configureCardCurrencies(cards);
                if(!trips.existsById("trip-expired-card"))trips.save(trip("trip-expired-card","customer-001","France","Nice",LocalDate.of(2026,10,2),LocalDate.of(2026,10,12),new BigDecimal("1900"),"USD","card-expiring",Enums.TripStatus.PLANNED));
                seedTravelHistory(trips);curateDemoData(trips,txs,alerts,cases);seedOpenAlert(alerts);seedTrackingCase(cases);return;
            }
            accounts.save(Account.builder().id("account-001").customerId("customer-001").accountType("CHECKING").currency("USD").availableBalance(new BigDecimal("8500")).status(Enums.AccountStatus.ACTIVE).build());
            accounts.save(Account.builder().id("account-002").customerId("customer-001").accountType("SAVINGS").currency("USD").availableBalance(new BigDecimal("15000")).status(Enums.AccountStatus.ACTIVE).build());
            accounts.save(Account.builder().id("account-low").customerId("customer-001").accountType("CHECKING").currency("USD").availableBalance(new BigDecimal("200")).status(Enums.AccountStatus.ACTIVE).build());
            accounts.save(Account.builder().id("account-frozen").customerId("customer-001").accountType("SAVINGS").currency("USD").availableBalance(new BigDecimal("5000")).status(Enums.AccountStatus.FROZEN).build());
            accounts.save(Account.builder().id("account-201").customerId("customer-002").accountType("CHECKING").currency("USD").availableBalance(new BigDecimal("7000")).status(Enums.AccountStatus.ACTIVE).build());
            cards.save(card("card-001","customer-001","**** 1234","VISA",12,2029,true,Enums.CardStatus.ACTIVE,"account-001"));
            cards.save(card("card-002","customer-001","**** 5678","MASTERCARD",8,2029,false,Enums.CardStatus.ACTIVE,"account-001"));
            cards.save(card("card-expiring","customer-001","**** 9012","VISA",8,2026,true,Enums.CardStatus.ACTIVE,"account-002"));
            cards.save(card("card-frozen","customer-001","**** 3456","MASTERCARD",10,2028,true,Enums.CardStatus.FROZEN,"account-frozen"));
            cards.save(card("card-backup","customer-001","**** 7788","VISA",6,2030,true,Enums.CardStatus.ACTIVE,"account-low"));
            cards.save(card("card-201","customer-002","**** 2201","VISA",6,2030,true,Enums.CardStatus.ACTIVE,"account-201"));
            configureCardCurrencies(cards);
            trips.save(trip("trip-tokyo","customer-001","Japan","Tokyo",LocalDate.of(2026,8,10),LocalDate.of(2026,8,18),new BigDecimal("2500"),"USD","card-002",Enums.TripStatus.PLANNED));
            trips.save(trip("trip-paris","customer-001","France","Paris",LocalDate.of(2026,9,5),LocalDate.of(2026,9,12),new BigDecimal("3200"),"USD","card-001",Enums.TripStatus.PLANNED));
            trips.save(trip("trip-singapore","customer-001","Singapore","Singapore",LocalDate.of(2025,5,1),LocalDate.of(2025,5,8),new BigDecimal("1800"),"USD","card-001",Enums.TripStatus.COMPLETED));
            trips.save(trip("trip-expired-card","customer-001","France","Nice",LocalDate.of(2026,10,2),LocalDate.of(2026,10,12),new BigDecimal("1900"),"USD","card-expiring",Enums.TripStatus.PLANNED));
            trips.save(trip("trip-frozen-card","customer-001","Singapore","Singapore",LocalDate.of(2026,11,3),LocalDate.of(2026,11,10),new BigDecimal("2200"),"USD","card-frozen",Enums.TripStatus.PLANNED));
            trips.save(trip("trip-low-balance","customer-001","United Kingdom","London",LocalDate.of(2026,12,5),LocalDate.of(2026,12,12),new BigDecimal("3000"),"USD","card-backup",Enums.TripStatus.PLANNED));
            seedTravelHistory(trips);
            saveTx(txs,"txn-hotel","trip-tokyo","Tokyo Hotel","Japan","HOTEL","420.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-12T13:30:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-dining","trip-tokyo","Sushi House","Japan","DINING","82.50",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-13T11:30:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-atm","trip-tokyo","Shibuya ATM","Japan","CASH","150.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-13T13:30:00Z"),Enums.TransactionType.ATM_WITHDRAWAL);
            saveTx(txs,"txn-declined","trip-tokyo","Tokyo Electronics","Japan","SHOPPING","900.00",Enums.TransactionStatus.DECLINED,"LIMIT_EXCEEDED",Instant.parse("2026-08-14T09:30:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-refund","trip-tokyo","Tokyo Hotel Refund","Japan","HOTEL","120.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-15T09:30:00Z"),Enums.TransactionType.REFUND);
            saveTx(txs,"txn-reversed","trip-tokyo","Metro Preauthorisation","Japan","TRANSPORT","45.00",Enums.TransactionStatus.REVERSED,null,Instant.parse("2026-08-15T11:00:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-duplicate-a","trip-tokyo","Ginza Department Store","Japan","SHOPPING","210.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-16T10:00:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-duplicate-b","trip-tokyo","Ginza Department Store","Japan","SHOPPING","210.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-16T10:04:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-country-mismatch","trip-tokyo","Paris Luxury Boutique","France","LUXURY","1800.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-16T12:00:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-outside-date","trip-tokyo","Osaka Rail","Japan","TRANSPORT","75.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-09-01T09:00:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-decline-2","trip-tokyo","Akihabara Camera","Japan","SHOPPING","950.00",Enums.TransactionStatus.DECLINED,"LIMIT_EXCEEDED",Instant.parse("2026-08-14T09:35:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-network-fail","trip-tokyo","Airport Taxi","Japan","TRANSPORT","65.00",Enums.TransactionStatus.DECLINED,"NETWORK_ERROR",Instant.parse("2026-08-14T09:40:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-insufficient","trip-tokyo","Kyoto Ryokan","Japan","HOTEL","1250.00",Enums.TransactionStatus.DECLINED,"INSUFFICIENT_FUNDS",Instant.parse("2026-08-17T08:00:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-overseas-off","trip-tokyo","Tokyo Pharmacy","Japan","HEALTH","48.00",Enums.TransactionStatus.DECLINED,"OVERSEAS_DISABLED",Instant.parse("2026-08-17T08:10:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-online-off","trip-tokyo","Rail Pass Online","Japan","TRANSPORT","120.00",Enums.TransactionStatus.DECLINED,"ONLINE_PAYMENT_DISABLED",Instant.parse("2026-08-17T08:20:00Z"),Enums.TransactionType.ONLINE_PURCHASE);
            saveTx(txs,"txn-atm-limit","trip-tokyo","Shinjuku ATM","Japan","CASH","800.00",Enums.TransactionStatus.DECLINED,"ATM_LIMIT_EXCEEDED",Instant.parse("2026-08-17T08:30:00Z"),Enums.TransactionType.ATM_WITHDRAWAL);
            saveTx(txs,"txn-fraud-block","trip-tokyo","Luxury Watch Store","Japan","LUXURY","2100.00",Enums.TransactionStatus.DECLINED,"FRAUD_BLOCK",Instant.parse("2026-08-17T08:40:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-invalid-pin","trip-tokyo","Family Mart","Japan","GROCERIES","32.00",Enums.TransactionStatus.DECLINED,"INVALID_PIN",Instant.parse("2026-08-17T08:50:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-merchant-unsupported","trip-tokyo","Local Transit Kiosk","Japan","TRANSPORT","18.00",Enums.TransactionStatus.DECLINED,"MERCHANT_NOT_SUPPORTED",Instant.parse("2026-08-17T09:00:00Z"),Enums.TransactionType.PURCHASE);
            saveTx(txs,"txn-do-not-honor","trip-tokyo","Department Store","Japan","SHOPPING","640.00",Enums.TransactionStatus.DECLINED,"DO_NOT_HONOR",Instant.parse("2026-08-17T09:10:00Z"),Enums.TransactionType.PURCHASE);
            alerts.save(FraudAlert.builder().id("alert-open").customerId("customer-001").tripId("trip-tokyo").transactionId("txn-declined").cardId("card-002").riskScore(68).riskLevel(Enums.RiskLevel.HIGH).decision(Enums.FraudDecision.REQUIRE_CONFIRMATION).reasonCodes("HIGH_VALUE_TRANSACTION,MULTIPLE_DECLINES").customerMessage("We noticed an unusual payment attempt in Tokyo.").status(Enums.AlertStatus.OPEN).createdAt(Instant.now()).customerResponse(Enums.CustomerResponse.NONE).build());
            alerts.save(FraudAlert.builder().id("alert-safe").customerId("customer-001").tripId("trip-tokyo").transactionId("txn-hotel").cardId("card-002").riskScore(54).riskLevel(Enums.RiskLevel.HIGH).decision(Enums.FraudDecision.REQUIRE_CONFIRMATION).reasonCodes("HIGH_VALUE_TRANSACTION").customerMessage("Previously reviewed activity.").status(Enums.AlertStatus.CONFIRMED_SAFE).createdAt(Instant.now().minusSeconds(86400)).resolvedAt(Instant.now()).customerResponse(Enums.CustomerResponse.CONFIRM).build());
            alerts.save(FraudAlert.builder().id("alert-reported").customerId("customer-001").tripId("trip-tokyo").transactionId("txn-country-mismatch").cardId("card-002").riskScore(91).riskLevel(Enums.RiskLevel.CRITICAL).decision(Enums.FraudDecision.BLOCK_AND_ALERT).reasonCodes("OUTSIDE_DESTINATION,HIGH_VALUE_TRANSACTION,CUSTOMER_PROFILE_ANOMALY").customerMessage("A high-value purchase appeared outside your travel destination.").status(Enums.AlertStatus.REPORTED_FRAUD).createdAt(Instant.now().minusSeconds(172800)).resolvedAt(Instant.now().minusSeconds(170000)).customerResponse(Enums.CustomerResponse.REPORT).build());
            curateDemoData(trips,txs,alerts,cases);seedOpenAlert(alerts);seedTrackingCase(cases);
        };
    }
    private Card card(String id,String customer,String masked,String type,int month,int year,boolean overseas,Enums.CardStatus status,String account){
        return Card.builder().id(id).customerId(customer).maskedCardNumber(masked).cardType(type).expiryMonth(month).expiryYear(year).status(status).overseasPaymentsEnabled(overseas).onlinePaymentsEnabled(true).contactlessEnabled(true).dailyPaymentLimit(new BigDecimal("1500")).dailyWithdrawalLimit(new BigDecimal("500")).linkedAccountId(account).build();}
    private Trip trip(String id,String customer,String country,String city,LocalDate start,LocalDate end,BigDecimal budget,String currency,String card,Enums.TripStatus status){
        return Trip.builder().id(id).customerId(customer).destinationCountry(country).destinationCity(city).startDate(start).endDate(end).budget(budget).budgetCurrency(currency).preferredCardId(card).status(status).createdAt(Instant.now()).updatedAt(Instant.now()).build();}
    private void saveTx(TransactionRepository r,String id,String trip,String merchant,String country,String cat,String amount,Enums.TransactionStatus status,String failure,Instant time,Enums.TransactionType type){
        BigDecimal a=new BigDecimal(amount);r.save(TravelTransaction.builder().transactionId(id).customerId("customer-001").tripId(trip).cardId("card-002").merchantName(merchant).merchantCountry(country).merchantCity("Tokyo").merchantCategory(cat).originalAmount(a).originalCurrency("USD").billingAmount(a).billingCurrency("USD").exchangeRate(BigDecimal.ONE).transactionTime(time).transactionType(type).status(status).failureCode(failure)
                .recoveryStatus(status==Enums.TransactionStatus.DECLINED?("NETWORK_ERROR".equals(failure)?Enums.RecoveryStatus.RETRY_AVAILABLE:Enums.RecoveryStatus.ACTION_REQUIRED):null)
                .recoveryUpdatedAt(status==Enums.TransactionStatus.DECLINED?Instant.now():null).createdAt(Instant.now()).build());}
    private void seedFailureCases(TransactionRepository txs){
        if(!txs.existsById("txn-declined"))saveTx(txs,"txn-declined","trip-tokyo","Tokyo Electronics","Japan","SHOPPING","900.00",Enums.TransactionStatus.DECLINED,"LIMIT_EXCEEDED",Instant.parse("2026-08-14T09:30:00Z"),Enums.TransactionType.PURCHASE);
        if(!txs.existsById("txn-network-fail"))saveTx(txs,"txn-network-fail","trip-tokyo","Airport Taxi","Japan","TRANSPORT","65.00",Enums.TransactionStatus.DECLINED,"NETWORK_ERROR",Instant.parse("2026-08-14T09:40:00Z"),Enums.TransactionType.PURCHASE);
        if(!txs.existsById("txn-insufficient"))saveTx(txs,"txn-insufficient","trip-tokyo","Kyoto Ryokan","Japan","HOTEL","1250.00",Enums.TransactionStatus.DECLINED,"INSUFFICIENT_FUNDS",Instant.parse("2026-08-17T08:00:00Z"),Enums.TransactionType.PURCHASE);
        if(!txs.existsById("txn-fraud-block"))saveTx(txs,"txn-fraud-block","trip-tokyo","Luxury Watch Store","Japan","LUXURY","2100.00",Enums.TransactionStatus.DECLINED,"FRAUD_BLOCK",Instant.parse("2026-08-17T08:40:00Z"),Enums.TransactionType.PURCHASE);
    }
    private void curateDemoData(TripRepository trips,TransactionRepository txs,FraudAlertRepository alerts,SupportCaseRepository cases){
        trips.deleteAllById(List.of("trip-frozen-card","trip-low-balance"));
        List<String> temporaryIds=txs.findAll().stream().map(TravelTransaction::getTransactionId)
                .filter(id->id.startsWith("test-")).toList();
        alerts.findAll().stream().filter(a->temporaryIds.contains(a.getTransactionId())).forEach(alerts::delete);
        cases.findAll().stream().filter(c->temporaryIds.contains(c.getTransactionId())).forEach(cases::delete);
        txs.deleteAllById(temporaryIds);
        txs.deleteAllById(List.of("txn-decline-2","txn-overseas-off","txn-online-off","txn-atm-limit",
                "txn-invalid-pin","txn-merchant-unsupported","txn-do-not-honor","txn-refund","txn-reversed",
                "txn-duplicate-b","txn-outside-date"));
        seedCuratedTransactions(txs);
        txs.findAll().stream().filter(t->t.getStatus()==Enums.TransactionStatus.DECLINED&&t.getRecoveryStatus()==null).forEach(t->{
            t.setRecoveryStatus("NETWORK_ERROR".equals(t.getFailureCode())?Enums.RecoveryStatus.RETRY_AVAILABLE:Enums.RecoveryStatus.ACTION_REQUIRED);
            t.setRecoveryUpdatedAt(Instant.now());txs.save(t);
        });
    }
    private void seedCuratedTransactions(TransactionRepository txs){
        saveTx(txs,"txn-hotel","trip-tokyo","Tokyo Hotel","Japan","HOTEL","420.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-12T13:30:00Z"),Enums.TransactionType.PURCHASE);
        saveTx(txs,"txn-dining","trip-tokyo","Sushi House","Japan","DINING","82.50",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-13T11:30:00Z"),Enums.TransactionType.PURCHASE);
        saveTx(txs,"txn-atm","trip-tokyo","Shibuya ATM","Japan","CASH","150.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-13T13:30:00Z"),Enums.TransactionType.ATM_WITHDRAWAL);
        saveTx(txs,"txn-duplicate-a","trip-tokyo","Ginza Department Store","Japan","SHOPPING","210.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-16T10:00:00Z"),Enums.TransactionType.PURCHASE);
        saveTx(txs,"txn-country-mismatch","trip-tokyo","Paris Luxury Boutique","France","LUXURY","1800.00",Enums.TransactionStatus.APPROVED,null,Instant.parse("2026-08-16T12:00:00Z"),Enums.TransactionType.PURCHASE);
        saveTx(txs,"txn-declined","trip-tokyo","Tokyo Electronics","Japan","SHOPPING","900.00",Enums.TransactionStatus.DECLINED,"LIMIT_EXCEEDED",Instant.parse("2026-08-14T09:30:00Z"),Enums.TransactionType.PURCHASE);
        saveTx(txs,"txn-network-fail","trip-tokyo","Airport Taxi","Japan","TRANSPORT","65.00",Enums.TransactionStatus.DECLINED,"NETWORK_ERROR",Instant.parse("2026-08-14T09:40:00Z"),Enums.TransactionType.PURCHASE);
        saveTx(txs,"txn-insufficient","trip-tokyo","Kyoto Ryokan","Japan","HOTEL","1250.00",Enums.TransactionStatus.DECLINED,"INSUFFICIENT_FUNDS",Instant.parse("2026-08-17T08:00:00Z"),Enums.TransactionType.PURCHASE);
        saveTx(txs,"txn-fraud-block","trip-tokyo","Luxury Watch Store","Japan","LUXURY","2100.00",Enums.TransactionStatus.DECLINED,"FRAUD_BLOCK",Instant.parse("2026-08-17T08:40:00Z"),Enums.TransactionType.PURCHASE);
        saveCompletedTripTx(txs,"txn-sg-hotel","Marina Bay Hotel","HOTEL","840.00",Instant.parse("2025-05-02T12:00:00Z"),false);
        saveCompletedTripTx(txs,"txn-sg-dining","Lau Pa Sat","DINING","180.00",Instant.parse("2025-05-04T12:30:00Z"),false);
        saveCompletedTripTx(txs,"txn-sg-shopping","Orchard Store","SHOPPING","310.00",Instant.parse("2025-05-06T09:15:00Z"),false);
        saveCompletedTripTx(txs,"txn-sg-recovered","Sentosa Tickets","ENTERTAINMENT","95.00",Instant.parse("2025-05-07T10:10:00Z"),true);
    }
    private void saveCompletedTripTx(TransactionRepository txs,String id,String merchant,String category,String amount,Instant time,boolean recovered){
        BigDecimal value=new BigDecimal(amount);
        txs.save(TravelTransaction.builder().transactionId(id).customerId("customer-001").tripId("trip-singapore").cardId("card-001")
                .merchantName(merchant).merchantCountry("Singapore").merchantCity("Singapore").merchantCategory(category)
                .originalAmount(value).originalCurrency("USD").billingAmount(value).billingCurrency("USD").exchangeRate(BigDecimal.ONE)
                .transactionTime(time).transactionType(Enums.TransactionType.PURCHASE).status(Enums.TransactionStatus.APPROVED)
                .failureCode(recovered?"NETWORK_ERROR":null)
                .recoveryStatus(recovered?Enums.RecoveryStatus.COMPLETED_AFTER_RETRY:null)
                .recoveryAttemptCount(recovered?1:0).recoveryStartedAt(recovered?time.plusSeconds(60):null)
                .recoveryCompletedAt(recovered?time.plusSeconds(90):null).recoveryUpdatedAt(recovered?time.plusSeconds(90):null)
                .createdAt(time).build());
    }
    private void seedTravelHistory(TripRepository trips){
        if(!trips.existsById("trip-singapore"))trips.save(trip("trip-singapore","customer-001","Singapore","Singapore",
                LocalDate.of(2025,5,1),LocalDate.of(2025,5,8),new BigDecimal("1800"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-singapore-return"))trips.save(trip("trip-singapore-return","customer-001","Singapore","Singapore",
                LocalDate.of(2023,8,12),LocalDate.of(2023,8,17),new BigDecimal("1500"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-new-york"))trips.save(trip("trip-new-york","customer-001","United States","New York",
                LocalDate.of(2024,10,6),LocalDate.of(2024,10,12),new BigDecimal("2400"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-barcelona"))trips.save(trip("trip-barcelona","customer-001","Spain","Barcelona",
                LocalDate.of(2024,4,15),LocalDate.of(2024,4,22),new BigDecimal("2100"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-sydney"))trips.save(trip("trip-sydney","customer-001","Australia","Sydney",
                LocalDate.of(2023,12,2),LocalDate.of(2023,12,11),new BigDecimal("3600"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-rome-history"))trips.save(trip("trip-rome-history","customer-001","Italy","Rome",
                LocalDate.of(2023,5,10),LocalDate.of(2023,5,18),new BigDecimal("2600"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-dubai-history"))trips.save(trip("trip-dubai-history","customer-001","United Arab Emirates","Dubai",
                LocalDate.of(2022,11,3),LocalDate.of(2022,11,9),new BigDecimal("3100"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-cape-town-history"))trips.save(trip("trip-cape-town-history","customer-001","South Africa","Cape Town",
                LocalDate.of(2022,3,12),LocalDate.of(2022,3,22),new BigDecimal("2800"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-rio-history"))trips.save(trip("trip-rio-history","customer-001","Brazil","Rio de Janeiro",
                LocalDate.of(2021,9,5),LocalDate.of(2021,9,14),new BigDecimal("2400"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-vancouver-history"))trips.save(trip("trip-vancouver-history","customer-001","Canada","Vancouver",
                LocalDate.of(2021,2,18),LocalDate.of(2021,2,25),new BigDecimal("2100"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-bangkok-history"))trips.save(trip("trip-bangkok-history","customer-001","Thailand","Bangkok",
                LocalDate.of(2020,8,2),LocalDate.of(2020,8,10),new BigDecimal("1900"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-thailand-2024"))trips.save(trip("trip-thailand-2024","customer-001","Thailand","Chiang Mai",
                LocalDate.of(2024,7,6),LocalDate.of(2024,7,12),new BigDecimal("1700"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-thailand-2022"))trips.save(trip("trip-thailand-2022","customer-001","Thailand","Phuket",
                LocalDate.of(2022,7,14),LocalDate.of(2022,7,21),new BigDecimal("2200"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-thailand-2018"))trips.save(trip("trip-thailand-2018","customer-001","Thailand","Bangkok",
                LocalDate.of(2018,11,3),LocalDate.of(2018,11,9),new BigDecimal("1600"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-reykjavik-history"))trips.save(trip("trip-reykjavik-history","customer-001","Iceland","Reykjavik",
                LocalDate.of(2019,12,7),LocalDate.of(2019,12,13),new BigDecimal("2700"),"USD","card-001",Enums.TripStatus.COMPLETED));
        if(!trips.existsById("trip-mexico-city-history"))trips.save(trip("trip-mexico-city-history","customer-001","Mexico","Mexico City",
                LocalDate.of(2019,4,20),LocalDate.of(2019,4,27),new BigDecimal("1800"),"USD","card-001",Enums.TripStatus.COMPLETED));
    }
    private void seedOpenAlert(FraudAlertRepository alerts){
        if(!alerts.existsById("alert-review-pending"))alerts.save(FraudAlert.builder().id("alert-review-pending")
                .customerId("customer-001").tripId("trip-tokyo").transactionId("txn-fraud-block").cardId("card-002")
                .riskScore(82).riskLevel(Enums.RiskLevel.CRITICAL).decision(Enums.FraudDecision.REQUIRE_CONFIRMATION)
                .reasonCodes("HIGH_VALUE_TRANSACTION,CUSTOMER_PROFILE_ANOMALY")
                .customerMessage("Please confirm this high-value watch purchase before we release the payment.")
                .status(Enums.AlertStatus.OPEN).createdAt(Instant.now()).customerResponse(Enums.CustomerResponse.NONE).build());
    }
    private void seedTrackingCase(SupportCaseRepository cases){
        SupportCase c=cases.findById("CASE-TRV-2048").orElseGet(()->SupportCase.builder().id("CASE-TRV-2048")
                .customerId("customer-001").tripId("trip-tokyo").transactionId("txn-country-mismatch")
                .type(Enums.CaseType.FRAUD_INVESTIGATION)
                .title("Fraud investigation · Paris Luxury Boutique")
                .createdAt(Instant.now().minusSeconds(259200)).build());
        c.setStatus(Enums.CaseStatus.RESOLVED);
        c.setCurrentUpdate("Investigation complete. The payment was confirmed as fraud and the temporary credit is now final.");
        c.setUpdatedAt(Instant.now().minusSeconds(3600));cases.save(c);
    }
    private void configureCardCurrencies(CardRepository cards){
        configureCard(cards,"card-001","USD","JPY,EUR,GBP,SGD,CNY");
        configureCard(cards,"card-002","USD","JPY,EUR,SGD");
        configureCard(cards,"card-expiring","USD","EUR,GBP");
        configureCard(cards,"card-frozen","USD","SGD");
        configureCard(cards,"card-backup","USD","JPY,EUR");
        configureCard(cards,"card-201","USD","JPY,EUR");
    }
    private void configureCard(CardRepository cards,String id,String mainCurrency,String supportedCurrencies){
        cards.findById(id).ifPresent(card->{
            card.setMainCurrency(mainCurrency);
            card.setSupportedCurrencies(supportedCurrencies);
            cards.save(card);
        });
    }
}
