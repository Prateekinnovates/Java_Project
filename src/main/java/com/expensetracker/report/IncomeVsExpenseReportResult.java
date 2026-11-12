package com.expensetracker.report;

import com.expensetracker.model.Category;
import com.expensetracker.util.NumberFormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OOP Concept: Encapsulation
 * Holds global income vs expense overview, savings rate, spending trends,
 * and top spending category insights.
 */
public class IncomeVsExpenseReportResult {

    public static class TopSpendingInsight {
        private final Category category;
        private final double amount;
        private final double percentage;

        public TopSpendingInsight(Category category, double amount, double percentage) {
            this.category = category;
            this.amount = Math.round(amount * 100.0) / 100.0;
            this.percentage = Math.round(percentage * 10.0) / 10.0;
        }

        public Category getCategory() {
            return category;
        }

        public double getAmount() {
            return amount;
        }

        public double getPercentage() {
            return percentage;
        }
    }

    private final double totalIncome;
    private final double totalExpense;
    private final double netSavings;
    private final double savingsRate; // Percentage (0-100%)
    private final List<TopSpendingInsight> topCategories;
    private final List<String> trendDates;
    private final List<Double> trendExpenses;

    public IncomeVsExpenseReportResult(double totalIncome, double totalExpense, double netSavings,
                                       double savingsRate, List<TopSpendingInsight> topCategories,
                                       List<String> trendDates, List<Double> trendExpenses) {
        this.totalIncome = Math.round(totalIncome * 100.0) / 100.0;
        this.totalExpense = Math.round(totalExpense * 100.0) / 100.0;
        this.netSavings = Math.round(netSavings * 100.0) / 100.0;
        this.savingsRate = Math.round(savingsRate * 10.0) / 10.0;
        this.topCategories = new ArrayList<>(topCategories);
        this.trendDates = new ArrayList<>(trendDates);
        this.trendExpenses = new ArrayList<>(trendExpenses);
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public double getNetSavings() {
        return netSavings;
    }

    public double getSavingsRate() {
        return savingsRate;
    }

    public List<TopSpendingInsight> getTopCategories() {
        return Collections.unmodifiableList(topCategories);
    }

    public List<String> getTrendDates() {
        return Collections.unmodifiableList(trendDates);
    }

    public List<Double> getTrendExpenses() {
        return Collections.unmodifiableList(trendExpenses);
    }

    public String getTrendDatesJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < trendDates.size(); i++) {
            sb.append("\"").append(trendDates.get(i)).append("\"");
            if (i < trendDates.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getTrendExpensesJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < trendExpenses.size(); i++) {
            sb.append(NumberFormatUtil.formatAmount(trendExpenses.get(i)));
            if (i < trendExpenses.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
