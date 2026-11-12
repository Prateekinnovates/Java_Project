package com.expensetracker.report;

import com.expensetracker.util.NumberFormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OOP Concept: Encapsulation
 * Holds aggregated financial metrics grouped by month for bar chart and tabular presentation.
 */
public class MonthlyReportResult {
    private final List<String> months;
    private final List<Double> incomes;
    private final List<Double> expenses;
    private final List<Double> netSavings;
    private final double highestSpendingMonthAmount;
    private final String highestSpendingMonth;
    private final double averageMonthlyExpense;

    public MonthlyReportResult(List<String> months, List<Double> incomes, List<Double> expenses,
                               List<Double> netSavings, double highestSpendingMonthAmount,
                               String highestSpendingMonth, double averageMonthlyExpense) {
        this.months = new ArrayList<>(months);
        this.incomes = new ArrayList<>(incomes);
        this.expenses = new ArrayList<>(expenses);
        this.netSavings = new ArrayList<>(netSavings);
        this.highestSpendingMonthAmount = highestSpendingMonthAmount;
        this.highestSpendingMonth = highestSpendingMonth;
        this.averageMonthlyExpense = averageMonthlyExpense;
    }

    public List<String> getMonths() {
        return Collections.unmodifiableList(months);
    }

    public List<Double> getIncomes() {
        return Collections.unmodifiableList(incomes);
    }

    public List<Double> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public List<Double> getNetSavings() {
        return Collections.unmodifiableList(netSavings);
    }

    public double getHighestSpendingMonthAmount() {
        return highestSpendingMonthAmount;
    }

    public String getHighestSpendingMonth() {
        return highestSpendingMonth;
    }

    public double getAverageMonthlyExpense() {
        return averageMonthlyExpense;
    }

    public String getMonthsJson() {
        return toJsonStringList(months);
    }

    public String getIncomesJson() {
        return toJsonDoubleList(incomes);
    }

    public String getExpensesJson() {
        return toJsonDoubleList(expenses);
    }

    private static String toJsonStringList(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toJsonDoubleList(List<Double> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(NumberFormatUtil.formatAmount(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
