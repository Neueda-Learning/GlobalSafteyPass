package com.travelassistant.model;

public final class Enums {
    private Enums() {}
    public enum TripStatus { PLANNED, ACTIVE, COMPLETED, CANCELLED }
    public enum CardStatus { ACTIVE, FROZEN, EXPIRED, BLOCKED }
    public enum AccountStatus { ACTIVE, FROZEN, CLOSED }
    public enum TransactionType { PURCHASE, ATM_WITHDRAWAL, ONLINE_PURCHASE, REFUND }
    public enum TransactionStatus { APPROVED, DECLINED, PENDING, REVERSED }
    public enum Severity { INFO, WARNING, CRITICAL }
    public enum ReadinessStatus { READY, ACTION_REQUIRED, NOT_READY }
    public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
    public enum FraudDecision { ALLOW, MONITOR, REQUIRE_CONFIRMATION, BLOCK_AND_ALERT }
    public enum AlertStatus { OPEN, CONFIRMED_SAFE, REPORTED_FRAUD, CARD_FROZEN, CLOSED }
    public enum CustomerResponse { CONFIRM, REPORT, FREEZE, NONE }
    public enum ActionType { ADD_FUNDS, ENABLE_OVERSEAS_PAYMENT, ENABLE_ONLINE_PAYMENT, INCREASE_LIMIT, UNFREEZE_CARD, USE_ANOTHER_CARD, CONTACT_BANK, RETRY, CONFIRM_TRANSACTION }
    public enum CaseType { PAYMENT_SUPPORT, FRAUD_INVESTIGATION }
    public enum CaseStatus { SUBMITTED, UNDER_REVIEW, ACTION_REQUIRED, RESOLVED }
    public enum RecoveryStatus { PAYMENT_INTERRUPTED, RECOVERY_IN_PROGRESS, RETRY_AVAILABLE, COMPLETED_AFTER_RETRY, COMPLETED_WITH_ALTERNATE_CARD, ACTION_REQUIRED }
}
