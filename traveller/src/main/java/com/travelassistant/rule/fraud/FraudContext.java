package com.travelassistant.rule.fraud;
import com.travelassistant.model.*;
import java.math.BigDecimal;
import java.util.*;
public record FraudContext(Trip activeTrip, List<TravelTransaction> recentTransactions, Card card,
        Account account, BigDecimal customerAverageTransactionAmount, Set<String> customerKnownCountries,
        Set<String> customerKnownMerchantCategories) {}
