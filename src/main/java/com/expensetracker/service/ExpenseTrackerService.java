package com.expensetracker.service;

import com.expensetracker.exception.InvalidAmountException;
import com.expensetracker.exception.TransactionNotFoundException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Account;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryBudget;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;
import com.expensetracker.report.CategoryReport;
import com.expensetracker.report.CategoryReportResult;
import com.expensetracker.report.IncomeVsExpenseReport;
import com.expensetracker.report.IncomeVsExpenseReportResult;
import com.expensetracker.report.MonthlyReport;
import com.expensetracker.report.MonthlyReportResult;
import com.expensetracker.report.ReportGenerator;
import com.expensetracker.storage.StorageService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * OOP Concept: Service Layer & Facade Pattern
 * Acts as the single entry point coordinating business rules, financial calculations,
 * storage persistence, report generation strategies, and CSV import/export operations.
 */
public class ExpenseTrackerService {
    private final Account account;
    private final StorageService storageService;

    // Report strategies
    private final ReportGenerator<MonthlyReportResult> monthlyReportGenerator;
    private final ReportGenerator<CategoryReportResult> categoryReportGenerator;
    private final ReportGenerator<IncomeVsExpenseReportResult> incomeVsExpenseReportGenerator;

    public ExpenseTrackerService(StorageService storageService) {
        this.storageService = storageService;
        this.monthlyReportGenerator = new MonthlyReport();
        this.categoryReportGenerator = new CategoryReport();
        this.incomeVsExpenseReportGenerator = new IncomeVsExpenseReport();

        // Load persisted account or initialize with sample data if first time
        Account loaded = storageService.loadAccount();
        if (loaded != null && !loaded.getTransactions().isEmpty()) {
            this.account = loaded;
        } else {
            this.account = new Account("Personal Account");
            loadSampleData();
            storageService.saveAccount(this.account);
        }
    }

    public Account getAccount() {
        return account;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    // ==========================================
    // Transaction CRUD
    // ==========================================

    /**
     * Creates and adds a new transaction.
     */
    public Transaction addTransaction(String type, double amount, String date, Category category, String description) {
        String id = generateUniqueId();
        Transaction tx = createTransactionInstance(id, type, amount, date, category, description);
        account.addTransaction(tx);
        storageService.saveAccount(account);
        return tx;
    }

    /**
     * Updates an existing transaction.
     */
    public Transaction updateTransaction(String id, String type, double amount, String date, Category category, String description) {
        if (account.findById(id).isEmpty()) {
            throw new TransactionNotFoundException(id);
        }
        Transaction tx = createTransactionInstance(id, type, amount, date, category, description);
        account.updateTransaction(tx);
        storageService.saveAccount(account);
        return tx;
    }

    /**
     * Deletes a transaction by ID.
     */
    public void deleteTransaction(String id) {
        account.deleteTransaction(id);
        storageService.saveAccount(account);
    }

    public Transaction getTransaction(String id) {
        return account.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    // ==========================================
    // Polymorphic Factory Method
    // ==========================================

    private Transaction createTransactionInstance(String id, String type, double amount, String date, Category category, String description) {
        if (type == null || type.trim().isEmpty()) {
            throw new ValidationException("type", "Transaction type must be specified (INCOME or EXPENSE).");
        }
        String cleanType = type.trim().toUpperCase();
        if ("INCOME".equals(cleanType)) {
            return new Income(id, amount, date, description, category);
        } else if ("EXPENSE".equals(cleanType)) {
            return new Expense(id, amount, date, description, category);
        } else {
            throw new ValidationException("type", "Invalid transaction type: " + type + ". Must be INCOME or EXPENSE.");
        }
    }

    // ==========================================
    // Search, Filter & Sort (Streams & Lambdas)
    // ==========================================

    /**
     * Multi-criteria filtering and sorting using Java Streams.
     */
    public List<Transaction> filterTransactions(String query, Category category, String type,
                                               String fromDate, String toDate,
                                               String sortBy, boolean ascending) {
        return account.getTransactions().stream()
                // Search query match
                .filter(tx -> {
                    if (query == null || query.trim().isEmpty()) return true;
                    String q = query.trim().toLowerCase();
                    return tx.getDescription().toLowerCase().contains(q)
                            || tx.getCategory().getDisplayName().toLowerCase().contains(q)
                            || String.valueOf(tx.getAmount()).contains(q)
                            || tx.getDate().contains(q);
                })
                // Category filter
                .filter(tx -> category == null || tx.getCategory() == category)
                // Type filter (ALL, INCOME, EXPENSE)
                .filter(tx -> {
                    if (type == null || type.trim().isEmpty() || "ALL".equalsIgnoreCase(type.trim())) {
                        return true;
                    }
                    return tx.getType().equalsIgnoreCase(type.trim());
                })
                // Date range filters
                .filter(tx -> fromDate == null || fromDate.trim().isEmpty() || tx.getDate().compareTo(fromDate.trim()) >= 0)
                .filter(tx -> toDate == null || toDate.trim().isEmpty() || tx.getDate().compareTo(toDate.trim()) <= 0)
                // Sorting
                .sorted((tx1, tx2) -> {
                    int result;
                    if ("amount".equalsIgnoreCase(sortBy)) {
                        result = Double.compare(tx1.getAmount(), tx2.getAmount());
                    } else if ("category".equalsIgnoreCase(sortBy)) {
                        result = tx1.getCategory().getDisplayName().compareToIgnoreCase(tx2.getCategory().getDisplayName());
                    } else { // default: "date"
                        result = tx1.getDate().compareTo(tx2.getDate());
                    }
                    return ascending ? result : -result;
                })
                .collect(Collectors.toList());
    }

    // ==========================================
    // Budget Management
    // ==========================================

    public void setCategoryBudget(Category category, double limit) {
        account.setBudget(category, limit);
        storageService.saveAccount(account);
    }

    public double getCategoryBudget(Category category) {
        return account.getBudget(category);
    }

    /**
     * Evaluates budget compliance for all expense categories in a specific month.
     */
    public List<CategoryBudget> getBudgetStatusList(String yearMonth) {
        List<CategoryBudget> list = new ArrayList<>();
        for (Category cat : Category.values()) {
            if (cat.getType() == com.expensetracker.model.CategoryType.EXPENSE || cat.getType() == com.expensetracker.model.CategoryType.BOTH) {
                double limit = account.getBudget(cat);
                double spent = account.calculateCategoryMonthSpending(cat, yearMonth);
                if (limit > 0.0 || spent > 0.0) {
                    list.add(new CategoryBudget(cat, limit, spent));
                }
            }
        }
        list.sort((b1, b2) -> Double.compare(b2.getPercentageUsed(), b1.getPercentageUsed()));
        return list;
    }

    // ==========================================
    // Report Strategy Invocations
    // ==========================================

    public MonthlyReportResult generateMonthlyReport() {
        return monthlyReportGenerator.generate(account);
    }

    public CategoryReportResult generateCategoryReport() {
        return categoryReportGenerator.generate(account);
    }

    public IncomeVsExpenseReportResult generateIncomeVsExpenseReport() {
        return incomeVsExpenseReportGenerator.generate(account);
    }

    // ==========================================
    // CSV Export & Import
    // ==========================================

    /**
     * Exports transactions to CSV string.
     */
    public String exportToCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Type,Amount,Date,Category,Description\n");
        for (Transaction tx : account.getTransactions()) {
            sb.append(tx.toCsvRow()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Imports transactions from CSV content. Returns count of successfully imported records.
     */
    public int importFromCsv(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            throw new ValidationException("csv", "CSV content cannot be empty.");
        }

        String[] lines = csvContent.split("\r?\n");
        if (lines.length < 2) {
            throw new ValidationException("csv", "CSV does not contain any transaction rows.");
        }

        int importedCount = 0;
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Skip header if line 1
            if (lineNumber == 1 && (trimmed.toLowerCase().startsWith("id,") || trimmed.toLowerCase().startsWith("\"id\""))) {
                continue;
            }

            List<String> tokens = parseCsvLine(trimmed);
            if (tokens.size() < 5) {
                System.err.println("Line " + lineNumber + ": Insufficient CSV columns, skipping: " + trimmed);
                continue;
            }

            try {
                // Expected format: ID,Type,Amount,Date,Category,Description
                String id = tokens.size() > 0 && !tokens.get(0).isEmpty() ? tokens.get(0) : generateUniqueId();
                String type = tokens.size() > 1 ? tokens.get(1) : "EXPENSE";
                double amount = Double.parseDouble(tokens.get(2).replace("$", "").replace(",", "").trim());
                String date = tokens.get(3).trim();
                Category category = Category.fromString(tokens.get(4).trim());
                String desc = tokens.size() > 5 ? tokens.get(5) : "Imported transaction";

                // Ensure unique ID
                if (account.findById(id).isPresent()) {
                    id = generateUniqueId();
                }

                Transaction tx = createTransactionInstance(id, type, amount, date, category, desc);
                account.addTransaction(tx);
                importedCount++;
            } catch (Exception ex) {
                System.err.println("Line " + lineNumber + ": Error importing row: " + ex.getMessage());
            }
        }

        if (importedCount > 0) {
            storageService.saveAccount(account);
        }
        return importedCount;
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }

    // ==========================================
    // Clear & Sample Data
    // ==========================================

    public void clearAllData() {
        account.clearAll();
        storageService.clear();
    }

    public void loadSampleData() {
        account.clearAll();

        // Sample Budgets
        account.setBudget(Category.FOOD, 600.0);
        account.setBudget(Category.SHOPPING, 350.0);
        account.setBudget(Category.ENTERTAINMENT, 200.0);
        account.setBudget(Category.TRAVEL, 300.0);
        account.setBudget(Category.BILLS, 400.0);

        // Sample Transactions spanning recent months
        List<Transaction> samples = Arrays.asList(
                // Incomes
                new Income("tx-101", 4500.00, "2026-09-01", "Primary Tech Salary", Category.SALARY),
                new Income("tx-102", 750.00, "2026-09-15", "Freelance UI Design Project", Category.FREELANCE),
                new Income("tx-103", 4500.00, "2026-10-01", "Primary Tech Salary", Category.SALARY),
                new Income("tx-104", 320.00, "2026-10-18", "Stock Dividend Payout", Category.INVESTMENT),
                new Income("tx-105", 4500.00, "2026-11-01", "Primary Tech Salary", Category.SALARY),
                new Income("tx-106", 850.00, "2026-11-08", "Mobile App Consulting", Category.FREELANCE),

                // Expenses: September 2026
                new Expense("tx-201", 380.00, "2026-09-03", "Whole Foods Grocery Stockup", Category.FOOD),
                new Expense("tx-202", 220.00, "2026-09-07", "Monthly Metro & Rail Transit Pass", Category.TRAVEL),
                new Expense("tx-203", 160.00, "2026-09-10", "High-speed Internet & Electricity", Category.BILLS),
                new Expense("tx-204", 190.00, "2026-09-14", "Winter Wardrobe Sale", Category.SHOPPING),
                new Expense("tx-205", 85.00, "2026-09-20", "Cinema & Weekend Concert", Category.ENTERTAINMENT),

                // Expenses: October 2026
                new Expense("tx-301", 420.00, "2026-10-02", "Fresh Produce & Organic Market", Category.FOOD),
                new Expense("tx-302", 140.00, "2026-10-06", "Electricity & Water Bill", Category.BILLS),
                new Expense("tx-303", 290.00, "2026-10-12", "Wireless Noise-Cancelling Headphones", Category.SHOPPING),
                new Expense("tx-304", 175.00, "2026-10-19", "Dental Checkup & Prescriptions", Category.HEALTH),
                new Expense("tx-305", 210.00, "2026-10-23", "Weekend Road Trip Fuel & Tolls", Category.TRAVEL),
                new Expense("tx-306", 120.00, "2026-10-28", "Online Streaming & Games Subscription", Category.ENTERTAINMENT),

                // Expenses: November 2026 (Current Month)
                new Expense("tx-401", 340.00, "2026-11-02", "Monthly Grocery Store Run", Category.FOOD),
                new Expense("tx-402", 180.00, "2026-11-04", "Electric Utility & Fiber Internet", Category.BILLS),
                new Expense("tx-403", 215.00, "2026-11-06", "Airport Shuttle & Uber Rides", Category.TRAVEL),
                new Expense("tx-404", 280.00, "2026-11-07", "Running Shoes & Fitness Apparel", Category.SHOPPING),
                new Expense("tx-405", 95.00, "2026-11-09", "Annual Physical & Vitamins", Category.HEALTH),
                new Expense("tx-406", 65.00, "2026-11-10", "Dinner with Colleagues", Category.FOOD),
                new Expense("tx-407", 75.00, "2026-11-11", "Book Store & Tech Magazine", Category.EDUCATION)
        );

        for (Transaction tx : samples) {
            account.addTransaction(tx);
        }

        storageService.saveAccount(account);
    }

    private String generateUniqueId() {
        return "tx-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 900 + 100);
    }
}

