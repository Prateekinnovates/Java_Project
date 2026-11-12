package com.expensetracker.report;

import com.expensetracker.model.Account;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * OOP Concept: Concrete Strategy & Polymorphism (Implements ReportGenerator)
 * Aggregates monthly income and expense metrics using Java Streams and Collections.
 */
public class MonthlyReport implements ReportGenerator<MonthlyReportResult> {

    @Override
    public MonthlyReportResult generate(Account account) {
        if (account == null || account.getTransactions().isEmpty()) {
            return new MonthlyReportResult(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0, "N/A", 0.0
            );
        }

        // Group transactions by Year-Month (e.g. 2026-10, 2026-11)
        Map<String, List<Transaction>> byMonth = account.getTransactions().stream()
                .collect(Collectors.groupingBy(
                        Transaction::getYearMonth,
                        TreeMap::new, // Sorted chronologically
                        Collectors.toList()
                ));

        List<String> months = new ArrayList<>();
        List<Double> incomes = new ArrayList<>();
        List<Double> expenses = new ArrayList<>();
        List<Double> netSavings = new ArrayList<>();

        double highestExpense = 0.0;
        String highestExpenseMonth = "N/A";
        double totalExpensesSum = 0.0;

        for (Map.Entry<String, List<Transaction>> entry : byMonth.entrySet()) {
            String month = entry.getKey();
            List<Transaction> txs = entry.getValue();

            double monthIncome = txs.stream()
                    .filter(tx -> tx instanceof Income)
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            double monthExpense = txs.stream()
                    .filter(tx -> tx instanceof Expense)
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            double net = monthIncome - monthExpense;

            months.add(month);
            incomes.add(Math.round(monthIncome * 100.0) / 100.0);
            expenses.add(Math.round(monthExpense * 100.0) / 100.0);
            netSavings.add(Math.round(net * 100.0) / 100.0);

            totalExpensesSum += monthExpense;

            if (monthExpense > highestExpense) {
                highestExpense = monthExpense;
                highestExpenseMonth = month;
            }
        }

        double avgExpense = months.isEmpty() ? 0.0 : (totalExpensesSum / months.size());

        return new MonthlyReportResult(
                months,
                incomes,
                expenses,
                netSavings,
                Math.round(highestExpense * 100.0) / 100.0,
                highestExpenseMonth,
                Math.round(avgExpense * 100.0) / 100.0
        );
    }
}

