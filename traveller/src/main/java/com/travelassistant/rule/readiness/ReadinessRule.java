package com.travelassistant.rule.readiness;
import com.travelassistant.dto.ApiDtos.ReadinessRuleResult;
import com.travelassistant.model.*;
public interface ReadinessRule { ReadinessRuleResult evaluate(Trip trip, Card card, Account account); }
