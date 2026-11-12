package com.expensetracker.model;

import com.expensetracker.exception.TransactionNotFoundException;
import com.expensetracker.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OOP Concept: Encapsulation
 * Manages the user's financial ledger, enforcing state encapsulation and financial invariants.
 * All transaction modifications, calculations, and budget checks occur through verified methods.
 */
public class Account {
    private final String accountName;
    private final List<Transaction> transactions;
    private final Map<Category, Double> monthlyBudgets;

    public Account(String accountName) {
        if (accountName == null || accountName.trim().isEmpty()) {
            this.accountName = "Personal Account";
        } else {
            this.accountName = accountName.trim();
        }
        this.transactions = new ArrayList<>();
        this.monthlyBudgets = new EnumMap<>(Category.class);
    }

    public String getAccountName() {
        return accountName;
    }

    /**
     * Adds a transaction to the account.
     */
    public void addTransaction(Transaction tx) {
        if (tx == null) {
            throw new ValidationException("transaction", "Cannot add a null transaction.");
        }
        // Avoid duplicate IDs
        if (findById(tx.getId()).isPresent()) {
            throw new ValidationException("id", "Transaction with ID " + tx.getId() + " already exists.");
        }
        transactions.add(tx);
    }

    /**
     * Updates an existing transaction.
     * Throws TransactionNotFoundException if not found.
     */
    public void updateTransaction(Transaction updatedTx) {
        if (updatedTx == null) {
            throw new ValidationException("transaction", "Cannot update with a null transaction.");
        }
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId().equals(updatedTx.getId())) {
                transactions.set(i, updatedTx);
                return;
            }
        }
        throw new TransactionNotFoundException(updatedTx.getId());
    }

    /**
     * Deletes a transaction by ID.
     * Throws TransactionNotFoundException if not found.
     */
    public void deleteTransaction(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "Transaction ID cannot be empty.");
        }
        boolean removed = transactions.removeIf(tx -> tx.getId().equals(id.trim()));
        if (!removed) {
            throw new TransactionNotFoundException(id);
        }
    }

    /**
     * Finds a transaction by ID.
     */
    public Optional<Transaction> findById(String id) {
        if (id == null) return Optional.empty();
        return transactions.stream()
                .filter(tx -> tx.getId().equals(id.trim()))
                .findFirst();
    }

    /**
     * Returns an unmodifiable view of transactions to preserve encapsulation.
     */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /**
     * Polymorphic calculation of current balance.
     * Evaluates tx.getSignedAmount() for each transaction.
     */
    public double calculateBalance() {
        double balance = 0.0;
        for (Transaction tx : transactions) {
            balance += tx.getSignedAmount();
        }
        return Math.round(balance * 100.0) / 100.0;
    }

    /**
     * Calculates lifetime or total recorded income using Java Streams.
     */
    public double calculateTotalIncome() {
        double total = transactions.stream()
                .filter(tx -> tx instanceof Income)
                .mapToDouble(Transaction::getAmount)
                .sum();
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Calculates lifetime or total recorded expense using Java Streams.
     */
    public double calculateTotalExpense() {
        double total = transactions.stream()
                .filter(tx -> tx instanceof Expense)
                .mapToDouble(Transaction::getAmount)
                .sum();
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Calculates total spending for a specific year and month (e.g. "2026-11").
     */
    public double calculateMonthSpending(String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) return 0.0;
        double total = transactions.stream()
                .filter(tx -> tx instanceof Expense && yearMonth.equals(tx.getYearMonth()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Calculates total spending for a specific category in a specific year and month.
     */
    public double calculateCategoryMonthSpending(Category category, String yearMonth) {
        if (category == null || yearMonth == null) return 0.0;
        double total = transactions.stream()
                .filter(tx -> tx instanceof Expense
                        && tx.getCategory() == category
                        && yearMonth.equals(tx.getYearMonth()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Sets a monthly budget limit for a category.
     */
    public void setBudget(Category category, double limit) {
        if (category != null) {
            if (limit <= 0.0) {
                monthlyBudgets.remove(category);
            } else {
                monthlyBudgets.put(category, Math.round(limit * 100.0) / 100.0);
            }
        }
    }

    public double getBudget(Category category) {
        if (category == null) return 0.0;
        return monthlyBudgets.getOrDefault(category, 0.0);
    }

    public Map<Category, Double> getMonthlyBudgets() {
        return Collections.unmodifiableMap(monthlyBudgets);
    }

    public void clearAll() {
        transactions.clear();
        monthlyBudgets.clear();
    }
}

