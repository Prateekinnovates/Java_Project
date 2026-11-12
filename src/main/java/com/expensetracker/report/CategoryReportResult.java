package com.expensetracker.report;

import com.expensetracker.model.Category;
import com.expensetracker.util.NumberFormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OOP Concept: Encapsulation
 * Holds category spending distributions, percentages, colors, and budget compliance data.
 */
public class CategoryReportResult {

    public static class CategoryItem {
        private final Category category;
        private final double totalAmount;
        private final double percentage;
        private final double budgetLimit;
        private final boolean isExceeded;

        public CategoryItem(Category category, double totalAmount, double percentage, double budgetLimit) {
            this.category = category;
            this.totalAmount = Math.round(totalAmount * 100.0) / 100.0;
            this.percentage = Math.round(percentage * 10.0) / 10.0;
            this.budgetLimit = budgetLimit;
            this.isExceeded = budgetLimit > 0.0 && totalAmount > budgetLimit;
        }

        public Category getCategory() {
            return category;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public double getPercentage() {
            return percentage;
        }

        public double getBudgetLimit() {
            return budgetLimit;
        }

        public boolean isExceeded() {
            return isExceeded;
        }
    }

    private final List<CategoryItem> items;
    private final double totalExpense;

    public CategoryReportResult(List<CategoryItem> items, double totalExpense) {
        this.items = new ArrayList<>(items);
        this.totalExpense = Math.round(totalExpense * 100.0) / 100.0;
    }

    public List<CategoryItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public String getLabelsJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append("\"").append(items.get(i).getCategory().getDisplayName()).append("\"");
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getAmountsJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append(NumberFormatUtil.formatAmount(items.get(i).getTotalAmount()));
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getColorsJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append("\"").append(items.get(i).getCategory().getColor()).append("\"");
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
