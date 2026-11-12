package com.expensetracker.report;

import com.expensetracker.model.Account;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OOP Concept: Concrete Strategy & Polymorphism (Implements ReportGenerator)
 * Computes category-level expense breakdown, percentages, and budget overages using Java Streams.
 */
public class CategoryReport implements ReportGenerator<CategoryReportResult> {

    @Override
    public CategoryReportResult generate(Account account) {
        if (account == null || account.getTransactions().isEmpty()) {
            return new CategoryReportResult(Collections.emptyList(), 0.0);
        }

        // Filter all expenses and group sum by Category
        Map<Category, Double> categoryTotals = account.getTransactions().stream()
                .filter(tx -> tx instanceof Expense)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        double totalExpense = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();

        List<CategoryReportResult.CategoryItem> items = new ArrayList<>();
        for (Map.Entry<Category, Double> entry : categoryTotals.entrySet()) {
            Category cat = entry.getKey();
            double amount = entry.getValue();
            double pct = totalExpense > 0.0 ? (amount / totalExpense) * 100.0 : 0.0;
            double budget = account.getBudget(cat);

            items.add(new CategoryReportResult.CategoryItem(cat, amount, pct, budget));
        }

        // Sort descending by total amount spent
        items.sort(Comparator.comparingDouble(CategoryReportResult.CategoryItem::getTotalAmount).reversed());

        return new CategoryReportResult(items, totalExpense);
    }
}

