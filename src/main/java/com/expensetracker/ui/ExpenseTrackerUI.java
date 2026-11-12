package com.expensetracker.ui;

import com.expensetracker.exception.ExpenseTrackerException;
import com.expensetracker.exception.InvalidAmountException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.interop.JSInterop;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryBudget;
import com.expensetracker.model.CategoryType;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;
import com.expensetracker.report.CategoryReportResult;
import com.expensetracker.report.IncomeVsExpenseReportResult;
import com.expensetracker.report.MonthlyReportResult;
import com.expensetracker.service.ExpenseTrackerService;
import com.expensetracker.util.NumberFormatUtil;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

import java.util.List;

/**
 * OOP Concept: Presentation Layer & View Controller
 * Coordinates the DOM interface, event listeners, modal workflows, dynamic tables,
 * and passes analytical data to Chart.js.
 */
public class ExpenseTrackerUI {
    private final ExpenseTrackerService service;
    private final HTMLDocument document;

    private String activeTab = "dashboard";
    private String currentEditingTxId = null;

    // Filter states
    private String searchQuery = "";
    private String filterType = "ALL";
    private Category filterCategory = null;
    private String filterFromDate = "";
    private String filterToDate = "";
    private String sortBy = "date";
    private boolean sortAscending = false;

    public ExpenseTrackerUI(ExpenseTrackerService service) {
        this.service = service;
        this.document = HTMLDocument.current();
    }

    public void init() {
        // Register global window bridges for dynamic row actions
        registerWindowBridges();

        // Wire static DOM listeners (navigation, filters, forms)
        wireEventListeners();

        // Apply saved theme
        String savedTheme = service.getStorageService().getTheme();
        JSInterop.applyTheme(savedTheme);

        // Populate category dropdowns in modal and filter
        populateCategoryDropdowns();

        // Render current view
        showTab("dashboard");
    }

    // ==========================================
    // Event Wiring & Global Bridges
    // ==========================================

    private void registerWindowBridges() {
        JSInterop.registerGlobalFunction("editTx", (JSInterop.StringCallback) this::openEditModal);
        JSInterop.registerGlobalFunction("deleteTx", (JSInterop.StringCallback) this::handleDeleteTransaction);
        JSInterop.registerGlobalFunction("switchTab", (JSInterop.StringCallback) this::showTab);
        JSInterop.registerGlobalFunction("onCsvImported", (JSInterop.StringCallback) this::handleCsvImported);
    }

    private void wireEventListeners() {
        // Navigation links
        wireClick("nav-dashboard", e -> showTab("dashboard"));
        wireClick("nav-transactions", e -> showTab("transactions"));
        wireClick("nav-reports", e -> showTab("reports"));
        wireClick("nav-settings", e -> showTab("settings"));

        // Header quick actions
        wireClick("btn-quick-add", e -> openAddModal());
        wireClick("btn-theme-toggle", e -> toggleTheme());

        // Modal triggers & actions
        wireClick("btn-open-modal", e -> openAddModal());
        wireClick("modal-close-btn", e -> closeModal());
        wireClick("btn-cancel-modal", e -> closeModal());
        wireClick("form-transaction-save", e -> handleSaveTransaction());

        // Transactions filters
        wireInput("filter-search", e -> {
            this.searchQuery = getInputValue("filter-search");
            renderTransactionsTable();
        });

        wireChange("filter-type", e -> {
            this.filterType = getSelectValue("filter-type");
            renderTransactionsTable();
        });

        wireChange("filter-category", e -> {
            String val = getSelectValue("filter-category");
            this.filterCategory = "ALL".equalsIgnoreCase(val) || val.isEmpty() ? null : Category.fromString(val);
            renderTransactionsTable();
        });

        wireChange("filter-from-date", e -> {
            this.filterFromDate = getInputValue("filter-from-date");
            renderTransactionsTable();
        });

        wireChange("filter-to-date", e -> {
            this.filterToDate = getInputValue("filter-to-date");
            renderTransactionsTable();
        });

        wireChange("filter-sort-by", e -> {
            String val = getSelectValue("filter-sort-by");
            switch (val) {
                case "date-asc":
                    this.sortBy = "date";
                    this.sortAscending = true;
                    break;
                case "amount-desc":
                    this.sortBy = "amount";
                    this.sortAscending = false;
                    break;
                case "amount-asc":
                    this.sortBy = "amount";
                    this.sortAscending = true;
                    break;
                default: // date-desc
                    this.sortBy = "date";
                    this.sortAscending = false;
                    break;
            }
            renderTransactionsTable();
        });

        wireClick("btn-reset-filters", e -> {
            setInputValue("filter-search", "");
            setSelectValue("filter-type", "ALL");
            setSelectValue("filter-category", "ALL");
            setInputValue("filter-from-date", "");
            setInputValue("filter-to-date", "");
            setSelectValue("filter-sort-by", "date-desc");
            this.searchQuery = "";
            this.filterType = "ALL";
            this.filterCategory = null;
            this.filterFromDate = "";
            this.filterToDate = "";
            this.sortBy = "date";
            this.sortAscending = false;
            renderTransactionsTable();
        });

        // CSV Export & Import
        wireClick("btn-export-csv", e -> handleCsvExport());
        wireClick("btn-export-csv-settings", e -> handleCsvExport());
        wireClick("btn-clear-all-data", e -> handleClearAllData());
        wireClick("btn-load-sample-data", e -> handleLoadSampleData());
    }

    // ==========================================
    // View Rendering
    // ==========================================

    public void showTab(String tabName) {
        this.activeTab = tabName;

        // Update nav active classes
        updateNavActive("nav-dashboard", "dashboard".equals(tabName));
        updateNavActive("nav-transactions", "transactions".equals(tabName));
        updateNavActive("nav-reports", "reports".equals(tabName));
        updateNavActive("nav-settings", "settings".equals(tabName));

        // Show/hide sections
        setElementVisible("tab-dashboard", "dashboard".equals(tabName));
        setElementVisible("tab-transactions", "transactions".equals(tabName));
        setElementVisible("tab-reports", "reports".equals(tabName));
        setElementVisible("tab-settings", "settings".equals(tabName));

        // Render contents of the active tab
        if ("dashboard".equals(tabName)) {
            renderDashboard();
        } else if ("transactions".equals(tabName)) {
            renderTransactionsTable();
        } else if ("reports".equals(tabName)) {
            renderReports();
        } else if ("settings".equals(tabName)) {
            renderSettingsAndBudgets();
        }
    }

    private void renderDashboard() {
        double balance = service.getAccount().calculateBalance();
        double income = service.getAccount().calculateTotalIncome();
        double expense = service.getAccount().calculateTotalExpense();

        String currentYearMonth = getCurrentYearMonth();
        double monthSpending = service.getAccount().calculateMonthSpending(currentYearMonth);

        // Update card values
        setElementText("card-balance-val", formatCurrency(balance));
        setElementText("card-income-val", formatCurrency(income));
        setElementText("card-expense-val", formatCurrency(expense));
        setElementText("card-month-expense-val", formatCurrency(monthSpending));

        // Balance badge style
        HTMLElement balBadge = document.getElementById("card-balance-badge");
        if (balBadge != null) {
            if (balance >= 0) {
                balBadge.setClassName("badge badge-success");
                balBadge.setInnerHTML("Surplus");
            } else {
                balBadge.setClassName("badge badge-danger");
                balBadge.setInnerHTML("Deficit");
            }
        }

        // Render Recent Transactions (Top 5)
        List<Transaction> recent = service.filterTransactions("", null, "ALL", "", "", "date", false);
        StringBuilder sb = new StringBuilder();
        if (recent.isEmpty()) {
            sb.append("<div class='empty-state'><p>No transactions recorded yet.</p></div>");
        } else {
            int limit = Math.min(recent.size(), 5);
            for (int i = 0; i < limit; i++) {
                Transaction tx = recent.get(i);
                boolean isIncome = tx instanceof Income;
                String sign = isIncome ? "+" : "-";
                String colorClass = isIncome ? "text-income" : "text-expense";
                String icon = tx.getCategory().getIcon();

                sb.append("<div class='recent-tx-item'>")
                  .append("  <div class='recent-tx-left'>")
                  .append("    <div class='category-icon-pill'>").append(icon).append("</div>")
                  .append("    <div>")
                  .append("      <div class='recent-tx-desc'>").append(escapeHtml(tx.getDescription())).append("</div>")
                  .append("      <div class='recent-tx-meta'>").append(tx.getCategory().getDisplayName()).append(" • ").append(tx.getDate()).append("</div>")
                  .append("    </div>")
                  .append("  </div>")
                  .append("  <div class='recent-tx-amount ").append(colorClass).append("'>")
                  .append(sign).append(formatCurrency(tx.getAmount()))
                  .append("  </div>")
                  .append("</div>");
            }
        }
        setElementHtml("dashboard-recent-transactions", sb.toString());

        // Render Budget Summary Cards on Dashboard
        renderDashboardBudgets(currentYearMonth);
    }

    private void renderDashboardBudgets(String yearMonth) {
        List<CategoryBudget> budgets = service.getBudgetStatusList(yearMonth);
        StringBuilder sb = new StringBuilder();
        if (budgets.isEmpty()) {
            sb.append("<div class='empty-state'><p>No category budgets set. You can set them in Budgets & Settings.</p></div>");
        } else {
            for (CategoryBudget b : budgets) {
                if (b.getMonthlyLimit() <= 0.0) continue;
                double pct = Math.min(b.getPercentageUsed(), 100.0);
                String barColor = b.isExceeded() ? "bg-danger" : (b.isNearLimit() ? "bg-warning" : "bg-primary");
                String warningText = b.isExceeded() ? "⚠️ Over Budget!" : (b.isNearLimit() ? "⚠️ 80%+ used" : "On track");
                String badgeClass = b.isExceeded() ? "badge badge-danger" : (b.isNearLimit() ? "badge badge-warning" : "badge badge-success");

                sb.append("<div class='budget-card-item'>")
                  .append("  <div class='budget-header'>")
                  .append("    <span class='budget-cat-name'>").append(b.getCategory().getFormattedName()).append("</span>")
                  .append("    <span class='").append(badgeClass).append("'>").append(warningText).append("</span>")
                  .append("  </div>")
                  .append("  <div class='budget-progress-track'>")
                  .append("    <div class='budget-progress-bar ").append(barColor).append("' style='width: ").append(pct).append("%;'></div>")
                  .append("  </div>")
                  .append("  <div class='budget-footer'>")
                  .append("    <span>Spent: ").append(formatCurrency(b.getSpentAmount())).append(" / ").append(formatCurrency(b.getMonthlyLimit())).append("</span>")
                  .append("    <span class='budget-pct'>").append(NumberFormatUtil.formatPercent(b.getPercentageUsed())).append("</span>")
                  .append("  </div>")
                  .append("</div>");
            }
        }
        setElementHtml("dashboard-budgets-list", sb.toString());
    }

    private void renderTransactionsTable() {
        List<Transaction> filtered = service.filterTransactions(
                searchQuery, filterCategory, filterType, filterFromDate, filterToDate, sortBy, sortAscending
        );

        setElementText("tx-count-badge", filtered.size() + " records");

        StringBuilder sb = new StringBuilder();
        if (filtered.isEmpty()) {
            sb.append("<tr><td colspan='6' class='text-center py-4 text-muted'>No transactions found matching your filters.</td></tr>");
        } else {
            for (Transaction tx : filtered) {
                boolean isIncome = tx instanceof Income;
                String typeBadgeClass = isIncome ? "badge badge-income" : "badge badge-expense";
                String amountClass = isIncome ? "text-income fw-bold" : "text-expense fw-bold";
                String sign = isIncome ? "+" : "-";

                sb.append("<tr>")
                  .append("  <td><span class='").append(typeBadgeClass).append("'>").append(tx.getType()).append("</span></td>")
                  .append("  <td><span class='cat-pill'>").append(tx.getCategory().getFormattedName()).append("</span></td>")
                  .append("  <td class='tx-desc-cell'>").append(escapeHtml(tx.getDescription())).append("</td>")
                  .append("  <td class='text-muted'>").append(tx.getDate()).append("</td>")
                  .append("  <td class='").append(amountClass).append("'>").append(sign).append(formatCurrency(tx.getAmount())).append("</td>")
                  .append("  <td class='tx-actions-cell'>")
                  .append("    <button class='btn-action btn-edit' onclick=\"window.editTx('").append(tx.getId()).append("')\" title='Edit Transaction'>✏️</button>")
                  .append("    <button class='btn-action btn-delete' onclick=\"window.deleteTx('").append(tx.getId()).append("')\" title='Delete Transaction'>🗑️</button>")
                  .append("  </td>")
                  .append("</tr>");
            }
        }
        setElementHtml("transactions-tbody", sb.toString());
    }

    private void renderReports() {
        // 1. Monthly Report Strategy
        MonthlyReportResult monthly = service.generateMonthlyReport();
        setElementText("report-highest-month", monthly.getHighestSpendingMonth() + " (" + formatCurrency(monthly.getHighestSpendingMonthAmount()) + ")");
        setElementText("report-avg-expense", formatCurrency(monthly.getAverageMonthlyExpense()));
        JSInterop.renderMonthlyBarChart("chart-monthly-bar", monthly.getMonthsJson(), monthly.getIncomesJson(), monthly.getExpensesJson());

        // 2. Category Report Strategy
        CategoryReportResult categoryResult = service.generateCategoryReport();
        JSInterop.renderCategoryDoughnut("chart-category-doughnut", categoryResult.getLabelsJson(), categoryResult.getAmountsJson(), categoryResult.getColorsJson());

        // 3. Income vs Expense Strategy (Trend Line & Top 3)
        IncomeVsExpenseReportResult ivResult = service.generateIncomeVsExpenseReport();
        setElementText("report-savings-rate", NumberFormatUtil.formatPercent(ivResult.getSavingsRate()));
        setElementText("report-net-savings", formatCurrency(ivResult.getNetSavings()));
        JSInterop.renderTrendLineChart("chart-spending-trend", ivResult.getTrendDatesJson(), ivResult.getTrendExpensesJson());

        // Render Top 3 Spending Categories Insight Cards
        StringBuilder topSb = new StringBuilder();
        List<IncomeVsExpenseReportResult.TopSpendingInsight> topCats = ivResult.getTopCategories();
        if (topCats.isEmpty()) {
            topSb.append("<div class='empty-state'><p>No expense data to analyze.</p></div>");
        } else {
            int rank = 1;
            for (IncomeVsExpenseReportResult.TopSpendingInsight top : topCats) {
                topSb.append("<div class='top-cat-card'>")
                     .append("  <div class='top-cat-rank'>#").append(rank++).append("</div>")
                     .append("  <div class='top-cat-details'>")
                     .append("    <div class='top-cat-name'>").append(top.getCategory().getFormattedName()).append("</div>")
                     .append("    <div class='top-cat-amount'>").append(formatCurrency(top.getAmount())).append("</div>")
                     .append("  </div>")
                     .append("  <div class='top-cat-pct'>").append(NumberFormatUtil.formatPercent(top.getPercentage())).append("</div>")
                     .append("</div>");
            }
        }
        setElementHtml("report-top-categories-list", topSb.toString());

        // Render Category Breakdown Table
        StringBuilder catTableSb = new StringBuilder();
        if (categoryResult.getItems().isEmpty()) {
            catTableSb.append("<tr><td colspan='5' class='text-center py-3 text-muted'>No expense records found.</td></tr>");
        } else {
            for (CategoryReportResult.CategoryItem item : categoryResult.getItems()) {
                String budgetStr = item.getBudgetLimit() > 0 ? formatCurrency(item.getBudgetLimit()) : "No limit";
                String statusBadge = item.isExceeded()
                        ? "<span class='badge badge-danger'>⚠️ Exceeded</span>"
                        : (item.getBudgetLimit() > 0 ? "<span class='badge badge-success'>Within Budget</span>" : "<span class='badge badge-secondary'>Uncapped</span>");

                catTableSb.append("<tr>")
                          .append("  <td>").append(item.getCategory().getFormattedName()).append("</td>")
                          .append("  <td class='fw-bold'>").append(formatCurrency(item.getTotalAmount())).append("</td>")
                          .append("  <td>").append(NumberFormatUtil.formatPercent(item.getPercentage())).append("</td>")
                          .append("  <td>").append(budgetStr).append("</td>")
                          .append("  <td>").append(statusBadge).append("</td>")
                          .append("</tr>");
            }
        }
        setElementHtml("report-category-tbody", catTableSb.toString());
    }

    private void renderSettingsAndBudgets() {
        StringBuilder sb = new StringBuilder();
        for (Category cat : Category.values()) {
            if (cat.getType() == CategoryType.EXPENSE || cat.getType() == CategoryType.BOTH) {
                double currentBudget = service.getCategoryBudget(cat);
                String inputId = "budget-input-" + cat.name();

                sb.append("<div class='budget-setting-row'>")
                  .append("  <div class='budget-setting-cat'>")
                  .append("    <span class='cat-icon'>").append(cat.getIcon()).append("</span>")
                  .append("    <span class='cat-title'>").append(cat.getDisplayName()).append("</span>")
                  .append("  </div>")
                  .append("  <div class='budget-setting-input-wrap'>")
                  .append("    <span class='currency-prefix'>$</span>")
                  .append("    <input type='number' id='").append(inputId).append("' class='form-control form-control-sm' ")
                  .append("           value='").append(currentBudget > 0 ? NumberFormatUtil.toFixed(currentBudget, 0) : "").append("' ")
                  .append("           placeholder='0 (No limit)' min='0' step='10'>")
                  .append("    <button class='btn btn-sm btn-outline' onclick=\"window.saveCategoryBudget('").append(cat.name()).append("')\">Save</button>")
                  .append("  </div>")
                  .append("</div>");
            }
        }
        setElementHtml("settings-budgets-container", sb.toString());

        // Register window callback for category budget save
        JSInterop.registerGlobalFunction("saveCategoryBudget", (JSInterop.StringCallback) catName -> {
            try {
                Category cat = Category.fromString(catName);
                String val = getInputValue("budget-input-" + cat.name());
                double limit = val.isEmpty() ? 0.0 : Double.parseDouble(val);
                service.setCategoryBudget(cat, limit);
                JSInterop.showToast("Updated budget for " + cat.getDisplayName(), "success");
            } catch (Exception ex) {
                JSInterop.showToast("Invalid budget amount: " + ex.getMessage(), "error");
            }
        });
    }

    // ==========================================
    // Modal & CRUD Operations
    // ==========================================

    public void openAddModal() {
        this.currentEditingTxId = null;
        setElementText("modal-title", "Add New Transaction");
        clearModalForm();
        setInputValue("modal-date", getTodayDate());
        setElementVisible("transaction-modal", true);
    }

    public void openEditModal(String txId) {
        try {
            Transaction tx = service.getTransaction(txId);
            this.currentEditingTxId = tx.getId();
            setElementText("modal-title", "Edit Transaction");

            setSelectValue("modal-type", tx.getType());
            setInputValue("modal-amount", String.valueOf(tx.getAmount()));
            setInputValue("modal-date", tx.getDate());
            setSelectValue("modal-category", tx.getCategory().name());
            setInputValue("modal-description", tx.getDescription());
            clearModalError();

            setElementVisible("transaction-modal", true);
        } catch (ExpenseTrackerException ex) {
            JSInterop.showToast("Error opening transaction: " + ex.getMessage(), "error");
        }
    }

    public void closeModal() {
        this.currentEditingTxId = null;
        clearModalError();
        setElementVisible("transaction-modal", false);
    }

    private void handleSaveTransaction() {
        try {
            clearModalError();

            String type = getSelectValue("modal-type");
            String amountStr = getInputValue("modal-amount");
            String date = getInputValue("modal-date");
            String catStr = getSelectValue("modal-category");
            String description = getInputValue("modal-description");

            if (amountStr == null || amountStr.trim().isEmpty()) {
                throw new InvalidAmountException("Please enter a valid amount.");
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr.trim());
            } catch (NumberFormatException e) {
                throw new InvalidAmountException("Amount must be a numeric value.", 0.0);
            }

            Category category = Category.fromString(catStr);

            if (currentEditingTxId == null) {
                // Add
                service.addTransaction(type, amount, date, category, description);
                JSInterop.showToast("Transaction added successfully!", "success");
            } else {
                // Update
                service.updateTransaction(currentEditingTxId, type, amount, date, category, description);
                JSInterop.showToast("Transaction updated successfully!", "success");
            }

            closeModal();
            refreshCurrentTab();

        } catch (ValidationException | InvalidAmountException ex) {
            showModalError(ex.getMessage());
        } catch (ExpenseTrackerException ex) {
            showModalError("Error: " + ex.getMessage());
        } catch (Exception ex) {
            showModalError("Unexpected error: " + ex.getMessage());
        }
    }

    private void handleDeleteTransaction(String id) {
        if (JSInterop.confirm("Are you sure you want to delete this transaction? This action cannot be undone.")) {
            try {
                service.deleteTransaction(id);
                JSInterop.showToast("Transaction deleted.", "info");
                refreshCurrentTab();
            } catch (ExpenseTrackerException ex) {
                JSInterop.showToast("Error deleting: " + ex.getMessage(), "error");
            }
        }
    }

    private void handleCsvExport() {
        try {
            String csv = service.exportToCsv();
            String filename = "expenses_export_" + getTodayDate() + ".csv";
            JSInterop.downloadFile(filename, csv);
            JSInterop.showToast("Exported transactions to " + filename, "success");
        } catch (Exception ex) {
            JSInterop.showToast("Export failed: " + ex.getMessage(), "error");
        }
    }

    private void handleCsvImported(String csvContent) {
        try {
            int count = service.importFromCsv(csvContent);
            JSInterop.showToast("Successfully imported " + count + " transactions!", "success");
            refreshCurrentTab();
        } catch (Exception ex) {
            JSInterop.showToast("CSV import error: " + ex.getMessage(), "error");
        }
    }

    private void handleClearAllData() {
        if (JSInterop.confirm("Are you sure you want to clear ALL transactions and budgets? This cannot be recovered!")) {
            service.clearAllData();
            JSInterop.showToast("All data cleared.", "info");
            refreshCurrentTab();
        }
    }

    private void handleLoadSampleData() {
        if (JSInterop.confirm("Reset account with sample test data? Existing entries will be replaced.")) {
            service.loadSampleData();
            JSInterop.showToast("Sample data loaded successfully!", "success");
            refreshCurrentTab();
        }
    }

    private void toggleTheme() {
        String current = service.getStorageService().getTheme();
        String next = "dark".equalsIgnoreCase(current) ? "light" : "dark";
        service.getStorageService().saveTheme(next);
        JSInterop.applyTheme(next);
        // Refresh charts on theme switch for optimal contrast
        if ("reports".equals(activeTab)) {
            renderReports();
        }
    }

    private void refreshCurrentTab() {
        showTab(activeTab);
    }

    // ==========================================
    // UI Helpers
    // ==========================================

    private void populateCategoryDropdowns() {
        StringBuilder modalOptions = new StringBuilder();
        StringBuilder filterOptions = new StringBuilder("<option value='ALL'>All Categories</option>");

        for (Category cat : Category.values()) {
            modalOptions.append("<option value='").append(cat.name()).append("'>")
                        .append(cat.getFormattedName()).append("</option>");
            filterOptions.append("<option value='").append(cat.name()).append("'>")
                         .append(cat.getFormattedName()).append("</option>");
        }

        setElementHtml("modal-category", modalOptions.toString());
        setElementHtml("filter-category", filterOptions.toString());
    }

    private void clearModalForm() {
        setSelectValue("modal-type", "EXPENSE");
        setInputValue("modal-amount", "");
        setInputValue("modal-description", "");
        setSelectValue("modal-category", Category.FOOD.name());
        clearModalError();
    }

    private void showModalError(String error) {
        HTMLElement errorDiv = document.getElementById("modal-error-message");
        if (errorDiv != null) {
            errorDiv.setInnerHTML(escapeHtml(error));
            errorDiv.getStyle().setProperty("display", "block");
        }
    }

    private void clearModalError() {
        HTMLElement errorDiv = document.getElementById("modal-error-message");
        if (errorDiv != null) {
            errorDiv.setInnerHTML("");
            errorDiv.getStyle().setProperty("display", "none");
        }
    }

    private void updateNavActive(String navId, boolean isActive) {
        HTMLElement el = document.getElementById(navId);
        if (el != null) {
            if (isActive) {
                el.getClassList().add("active");
            } else {
                el.getClassList().remove("active");
            }
        }
    }

    private void setElementVisible(String id, boolean visible) {
        HTMLElement el = document.getElementById(id);
        if (el != null) {
            el.getStyle().setProperty("display", visible ? "block" : "none");
        }
    }

    private void setElementText(String id, String text) {
        HTMLElement el = document.getElementById(id);
        if (el != null) {
            el.setTextContent(text);
        }
    }

    private void setElementHtml(String id, String html) {
        HTMLElement el = document.getElementById(id);
        if (el != null) {
            el.setInnerHTML(html);
        }
    }

    private String getInputValue(String id) {
        HTMLInputElement el = (HTMLInputElement) document.getElementById(id);
        return el != null ? el.getValue().trim() : "";
    }

    private void setInputValue(String id, String value) {
        HTMLInputElement el = (HTMLInputElement) document.getElementById(id);
        if (el != null) {
            el.setValue(value);
        }
    }

    private String getSelectValue(String id) {
        HTMLSelectElement el = (HTMLSelectElement) document.getElementById(id);
        return el != null ? el.getValue().trim() : "";
    }

    private void setSelectValue(String id, String value) {
        HTMLSelectElement el = (HTMLSelectElement) document.getElementById(id);
        if (el != null) {
            el.setValue(value);
        }
    }

    private void wireClick(String id, EventListener listener) {
        HTMLElement el = document.getElementById(id);
        if (el != null) {
            el.addEventListener("click", listener);
        }
    }

    private void wireInput(String id, EventListener listener) {
        HTMLElement el = document.getElementById(id);
        if (el != null) {
            el.addEventListener("input", listener);
        }
    }

    private void wireChange(String id, EventListener listener) {
        HTMLElement el = document.getElementById(id);
        if (el != null) {
            el.addEventListener("change", listener);
        }
    }

    private String formatCurrency(double amount) {
        return NumberFormatUtil.formatCurrency(amount);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private String getTodayDate() {
        return "2026-11-12";
    }

    private String getCurrentYearMonth() {
        return "2026-11";
    }
}

