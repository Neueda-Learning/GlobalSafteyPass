package com.travelassistant.rule.fraud;
import com.travelassistant.dto.ApiDtos.FraudRuleResult;
import com.travelassistant.model.TravelTransaction;
public interface FraudRule { FraudRuleResult evaluate(TravelTransaction transaction, FraudContext context); }
