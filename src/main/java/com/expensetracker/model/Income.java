package com.expensetracker.model;

/**
 * OOP Concept: Inheritance & Polymorphism
 * Represents positive cash inflow (Salary, Freelance, Investments, etc.).
 * Inherits all common transaction attributes from Transaction and specializes:
 * - getSignedAmount(): returns positive amount (+).
 * - getType(): returns "INCOME".
 */
public class Income extends Transaction {

    public Income(String id, double amount, String date, String description, Category category) {
        super(id, amount, date, description, category);
    }

    @Override
    public double getSignedAmount() {
        return getAmount();
    }

    @Override
    public String getType() {
        return "INCOME";
    }
}

