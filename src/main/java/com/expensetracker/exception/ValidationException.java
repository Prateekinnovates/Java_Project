package com.expensetracker.exception;

/**
 * OOP Concept: Inheritance & Encapsulation
 * Thrown when form input fails business validation rules (empty description, invalid date, etc.).
 */
public class ValidationException extends ExpenseTrackerException {
    private final String fieldName;

    public ValidationException(String fieldName, String message) {
        super(fieldName + ": " + message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}

