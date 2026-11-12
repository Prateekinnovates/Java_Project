package com.expensetracker.exception;

/**
 * OOP Concept: Inheritance & Encapsulation of Error State
 * Thrown when an invalid monetary amount is provided (e.g. <= 0, NaN, or non-numeric).
 */
public class InvalidAmountException extends ExpenseTrackerException {
    private final double attemptedAmount;

    public InvalidAmountException(String message) {
        super(message);
        this.attemptedAmount = 0.0;
    }

    public InvalidAmountException(String message, double attemptedAmount) {
        super(message + " (Attempted: " + attemptedAmount + ")");
        this.attemptedAmount = attemptedAmount;
    }

    public double getAttemptedAmount() {
        return attemptedAmount;
    }
}

