package com.expensetracker.exception;

/**
 * OOP Concept: Inheritance & Encapsulation of Error State
 * Thrown when an operation targets a transaction ID that does not exist in the account.
 */
public class TransactionNotFoundException extends ExpenseTrackerException {
    private final String transactionId;

    public TransactionNotFoundException(String transactionId) {
        super("Transaction with ID '" + transactionId + "' was not found.");
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}

