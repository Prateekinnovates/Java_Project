package com.expensetracker.exception;

/**
 * OOP Concept: Exception Hierarchy & Inheritance
 * Base custom exception for all Expense Tracker business errors.
 */
public class ExpenseTrackerException extends RuntimeException {
    public ExpenseTrackerException(String message) {
        super(message);
    }

    public ExpenseTrackerException(String message, Throwable cause) {
        super(message, cause);
    }
}

