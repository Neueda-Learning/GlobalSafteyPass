package com.travelassistant.integration;
import com.travelassistant.dto.ApiDtos.ExternalFraudResult;
import com.travelassistant.model.TravelTransaction;
import com.travelassistant.rule.fraud.FraudContext;
public interface FraudDetectionClient { ExternalFraudResult score(TravelTransaction transaction, FraudContext context); }
