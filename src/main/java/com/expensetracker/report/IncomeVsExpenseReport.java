package com.expensetracker.report;

import com.expensetracker.model.Account;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * OOP Concept: Concrete Strategy & Polymorphism (Implements ReportGenerator)
 * Evaluates high-level financial health: total income, total expense, net balance,
 * savings rate percentage, top 3 spending category insights, and spending trend over time.
 */
public class IncomeVsExpenseReport implements ReportGenerator<IncomeVsExpenseReportResult> {

    @Override
    public IncomeVsExpenseReportResult generate(Account account) {
        if (account == null || account.getTransactions().isEmpty()) {
            return new IncomeVsExpenseReportResult(
                    0.0, 0.0, 0.0, 0.0,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        double totalIncome = account.calculateTotalIncome();
        double totalExpense = account.calculateTotalExpense();
        double netSavings = totalIncome - totalExpense;

        double savingsRate = totalIncome > 0.0 ? ((totalIncome - totalExpense) / totalIncome) * 100.0 : 0.0;
        if (savingsRate < 0.0) savingsRate = 0.0;

        // Top 3 spending categories
        Map<Category, Double> expenseByCategory = account.getTransactions().stream()
                .filter(tx -> tx instanceof Expense)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        List<IncomeVsExpenseReportResult.TopSpendingInsight> topList = new ArrayList<>();
        double totalExp = totalExpense > 0.0 ? totalExpense : 1.0;

        expenseByCategory.entrySet().stream()
                .sorted(Map.Entry.<Category, Double>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    double amt = entry.getValue();
                    double pct = (amt / totalExp) * 100.0;
                    topList.add(new IncomeVsExpenseReportResult.TopSpendingInsight(entry.getKey(), amt, pct));
                });

        // Spending trend (Daily or Monthly expense aggregation)
        // Let's aggregate by Date for recent timeline or Month
        Map<String, Double> trendMap = account.getTransactions().stream()
                .filter(tx -> tx instanceof Expense)
                .collect(Collectors.groupingBy(
                        Transaction::getDate,
                        TreeMap::new,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        List<String> trendDates = new ArrayList<>(trendMap.keySet());
        List<Double> trendExpenses = new ArrayList<>();
        for (String d : trendDates) {
            trendExpenses.add(Math.round(trendMap.get(d) * 100.0) / 100.0);
        }

        return new IncomeVsExpenseReportResult(
                totalIncome,
                totalExpense,
                netSavings,
                savingsRate,
                topList,
                trendDates,
                trendExpenses
        );
    }
}

