package com.expensetracker.model;

/**
 * OOP Concept: Encapsulation
 * Encapsulates the budget limit, actual spending, and warning thresholds for a category.
 */
public class CategoryBudget {
    private final Category category;
    private double monthlyLimit;
    private double spentAmount;

    public CategoryBudget(Category category, double monthlyLimit, double spentAmount) {
        this.category = category;
        this.monthlyLimit = Math.max(0.0, monthlyLimit);
        this.spentAmount = Math.max(0.0, spentAmount);
    }

    public Category getCategory() {
        return category;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = Math.max(0.0, monthlyLimit);
    }

    public double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(double spentAmount) {
        this.spentAmount = Math.max(0.0, spentAmount);
    }

    public double getRemainingAmount() {
        return monthlyLimit - spentAmount;
    }

    public double getPercentageUsed() {
        if (monthlyLimit <= 0.0) {
            return 0.0;
        }
        return (spentAmount / monthlyLimit) * 100.0;
    }

    public boolean isExceeded() {
        return monthlyLimit > 0.0 && spentAmount > monthlyLimit;
    }

    public boolean isNearLimit() {
        return monthlyLimit > 0.0 && !isExceeded() && getPercentageUsed() >= 80.0;
    }
}

