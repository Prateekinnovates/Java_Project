package com.expensetracker.model;

import com.expensetracker.exception.InvalidAmountException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.util.NumberFormatUtil;

/**
 * OOP Concept: Abstraction, Encapsulation & Polymorphism
 * Abstract base class representing a general financial transaction.
 * Demonstrates:
 * - Abstraction: Cannot be instantiated directly; subclasses define specific transaction types.
 * - Encapsulation: Private fields with strict validation via getters and setters.
 * - Polymorphism: Declares abstract methods (getSignedAmount, getType) overridden by subclasses.
 */
public abstract class Transaction {
    private final String id;
    private double amount;
    private String date; // ISO format: YYYY-MM-DD
    private String description;
    private Category category;

    public Transaction(String id, double amount, String date, String description, Category category) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "Transaction ID cannot be null or empty.");
        }
        this.id = id.trim();
        setAmount(amount);
        setDate(date);
        setDescription(description);
        setCategory(category);
    }

    // Polymorphic methods to be implemented by Income and Expense
    public abstract double getSignedAmount();
    public abstract String getType();

    // Getters
    public String getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    // Setters with validation (Encapsulation)
    public void setAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0.0) {
            throw new InvalidAmountException("Amount must be a positive number greater than zero.", amount);
        }
        // Round to 2 decimal places
        this.amount = Math.round(amount * 100.0) / 100.0;
    }

    public void setDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new ValidationException("date", "Date cannot be empty.");
        }
        String trimmed = date.trim();
        // Validate basic YYYY-MM-DD format
        if (!trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new ValidationException("date", "Date must follow the format YYYY-MM-DD.");
        }
        this.date = trimmed;
    }

    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("description", "Description / note cannot be empty.");
        }
        this.description = description.trim();
    }

    public void setCategory(Category category) {
        if (category == null) {
            this.category = Category.OTHER;
        } else {
            this.category = category;
        }
    }

    /**
     * Extracts Year-Month (YYYY-MM) from the transaction date for monthly aggregations.
     */
    public String getYearMonth() {
        if (date != null && date.length() >= 7) {
            return date.substring(0, 7);
        }
        return "Unknown";
    }

    /**
     * Serializes this transaction into an RFC 4180 compliant CSV line.
     */
    public String toCsvRow() {
        String escapedDesc = description.replace("\"", "\"\"");
        return id + "," + getType() + "," + NumberFormatUtil.formatAmount(amount) + "," + date + "," + category.name() + ",\"" + escapedDesc + "\"";
    }

    @Override
    public String toString() {
        return "[" + getType() + "] " + id + ": " + NumberFormatUtil.formatAmount(amount) + " on " + date + " (" + category.getDisplayName() + ") - " + description;
    }
}
