package com.expensetracker.model;

/**
 * OOP Concept: Inheritance & Polymorphism
 * Represents negative cash outflow (Food, Bills, Shopping, Health, Travel, etc.).
 * Inherits all common transaction attributes from Transaction and specializes:
 * - getSignedAmount(): returns negative amount (-) to deduct from account balance.
 * - getType(): returns "EXPENSE".
 */
public class Expense extends Transaction {

    public Expense(String id, double amount, String date, String description, Category category) {
        super(id, amount, date, description, category);
    }

    @Override
    public double getSignedAmount() {
        return -getAmount();
    }

    @Override
    public String getType() {
        return "EXPENSE";
    }
}

