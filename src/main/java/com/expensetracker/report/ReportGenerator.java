package com.expensetracker.report;

import com.expensetracker.model.Account;

/**
 * OOP Concept: Interface & Strategy Pattern
 * Common contract for generating specialized financial analytics reports.
 * Allows the ExpenseTrackerService and UI to treat different reporting strategies
 * polymorphically.
 *
 * @param <T> The specific report result type produced by this generator.
 */
public interface ReportGenerator<T> {
    T generate(Account account);
}

