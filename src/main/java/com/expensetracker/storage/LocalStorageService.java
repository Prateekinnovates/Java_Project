package com.expensetracker.storage;

import com.expensetracker.model.Account;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;
import org.teavm.jso.JSBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OOP Concept: Implementation & Polymorphism
 * Implements StorageService using the browser's localStorage API via TeaVM JSO bridge.
 * Serializes and deserializes the financial Account, Transactions (polymorphically reconstructing
 * Income and Expense instances), and Category Budgets.
 */
public class LocalStorageService implements StorageService {
    private static final String STORAGE_KEY_DATA = "expense_tracker_data_v1";
    private static final String STORAGE_KEY_THEME = "expense_tracker_theme";

    @JSBody(params = { "key", "value" }, script = "try { localStorage.setItem(key, value); } catch(e){ console.error('LocalStorage write error', e); }")
    private static native void jsSetItem(String key, String value);

    @JSBody(params = { "key" }, script = "try { return localStorage.getItem(key); } catch(e){ return null; }")
    private static native String jsGetItem(String key);

    @JSBody(params = { "key" }, script = "try { localStorage.removeItem(key); } catch(e){}")
    private static native void jsRemoveItem(String key);

    @Override
    public boolean hasStoredData() {
        String data = jsGetItem(STORAGE_KEY_DATA);
        return data != null && !data.trim().isEmpty() && !data.trim().equals("{}");
    }

    @Override
    public Account loadAccount() {
        String json = jsGetItem(STORAGE_KEY_DATA);
        if (json == null || json.trim().isEmpty()) {
            return null; // Signals first run or empty state
        }
        try {
            return deserializeAccount(json);
        } catch (Exception e) {
            System.err.println("Error deserializing account from localStorage: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void saveAccount(Account account) {
        if (account == null) return;
        try {
            String json = serializeAccount(account);
            jsSetItem(STORAGE_KEY_DATA, json);
        } catch (Exception e) {
            System.err.println("Error saving account to localStorage: " + e.getMessage());
        }
    }

    @Override
    public void clear() {
        jsRemoveItem(STORAGE_KEY_DATA);
    }

    @Override
    public String getTheme() {
        String theme = jsGetItem(STORAGE_KEY_THEME);
        return (theme == null || theme.isEmpty()) ? "light" : theme;
    }

    @Override
    public void saveTheme(String theme) {
        if (theme != null) {
            jsSetItem(STORAGE_KEY_THEME, theme);
        }
    }

    // ==========================================
    // JSON Serialization & Deserialization
    // ==========================================

    private String serializeAccount(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"accountName\":").append(escapeJson(account.getAccountName())).append(",");
        sb.append("\"transactions\":[");

        List<Transaction> txList = account.getTransactions();
        for (int i = 0; i < txList.size(); i++) {
            Transaction tx = txList.get(i);
            sb.append("{");
            sb.append("\"id\":").append(escapeJson(tx.getId())).append(",");
            sb.append("\"type\":").append(escapeJson(tx.getType())).append(",");
            sb.append("\"amount\":").append(tx.getAmount()).append(",");
            sb.append("\"date\":").append(escapeJson(tx.getDate())).append(",");
            sb.append("\"category\":").append(escapeJson(tx.getCategory().name())).append(",");
            sb.append("\"description\":").append(escapeJson(tx.getDescription()));
            sb.append("}");
            if (i < txList.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("],");

        // Budgets map
        sb.append("\"budgets\":{");
        Map<Category, Double> budgets = account.getMonthlyBudgets();
        int bIdx = 0;
        int bSize = budgets.size();
        for (Map.Entry<Category, Double> entry : budgets.entrySet()) {
            sb.append(escapeJson(entry.getKey().name())).append(":").append(entry.getValue());
            if (++bIdx < bSize) {
                sb.append(",");
            }
        }
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    private Account deserializeAccount(String json) {
        Account account = new Account("Personal Account");
        if (json == null || json.trim().isEmpty()) {
            return account;
        }

        // Parse transactions array
        int txArrayStart = json.indexOf("\"transactions\":[");
        if (txArrayStart != -1) {
            int start = txArrayStart + "\"transactions\":[".length();
            int end = findMatchingBracket(json, start - 1, '[', ']');
            if (end != -1) {
                String txArrayContent = json.substring(start, end).trim();
                List<String> objects = splitJsonObjects(txArrayContent);
                for (String objJson : objects) {
                    try {
                        String id = extractJsonString(objJson, "id");
                        String type = extractJsonString(objJson, "type");
                        double amount = extractJsonDouble(objJson, "amount");
                        String date = extractJsonString(objJson, "date");
                        String catStr = extractJsonString(objJson, "category");
                        String desc = extractJsonString(objJson, "description");

                        Category category = Category.fromString(catStr);
                        Transaction tx;
                        if ("INCOME".equalsIgnoreCase(type)) {
                            tx = new Income(id, amount, date, desc, category);
                        } else {
                            tx = new Expense(id, amount, date, desc, category);
                        }
                        account.addTransaction(tx);
                    } catch (Exception ex) {
                        System.err.println("Skipping invalid transaction entry: " + ex.getMessage());
                    }
                }
            }
        }

        // Parse budgets
        int budgetsStart = json.indexOf("\"budgets\":{");
        if (budgetsStart != -1) {
            int start = budgetsStart + "\"budgets\":{".length();
            int end = findMatchingBracket(json, start - 1, '{', '}');
            if (end != -1) {
                String budgetsContent = json.substring(start, end).trim();
                if (!budgetsContent.isEmpty()) {
                    String[] entries = budgetsContent.split(",");
                    for (String entry : entries) {
                        String[] pair = entry.split(":");
                        if (pair.length == 2) {
                            String catKey = cleanQuotes(pair[0].trim());
                            try {
                                double limit = Double.parseDouble(pair[1].trim());
                                Category cat = Category.fromString(catKey);
                                account.setBudget(cat, limit);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }

        return account;
    }

    private static String escapeJson(String str) {
        if (str == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u0000".substring(0, 6 - hex.length())).append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static int findMatchingBracket(String s, int openIndex, char openChar, char closeChar) {
        int depth = 0;
        boolean inQuotes = false;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == openChar) depth++;
                else if (c == closeChar) {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static List<String> splitJsonObjects(String content) {
        List<String> list = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) return list;

        int depth = 0;
        int start = -1;
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        list.add(content.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return list;
    }

    private static String extractJsonString(String objJson, String key) {
        String search = "\"" + key + "\":";
        int idx = objJson.indexOf(search);
        if (idx == -1) return "";
        int start = idx + search.length();
        while (start < objJson.length() && Character.isWhitespace(objJson.charAt(start))) {
            start++;
        }
        if (start < objJson.length() && objJson.charAt(start) == '"') {
            start++;
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = start; i < objJson.length(); i++) {
                char c = objJson.charAt(i);
                if (escaped) {
                    sb.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    return sb.toString();
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static double extractJsonDouble(String objJson, String key) {
        String search = "\"" + key + "\":";
        int idx = objJson.indexOf(search);
        if (idx == -1) return 0.0;
        int start = idx + search.length();
        while (start < objJson.length() && (Character.isWhitespace(objJson.charAt(start)) || objJson.charAt(start) == '"')) {
            start++;
        }
        int end = start;
        while (end < objJson.length()) {
            char c = objJson.charAt(end);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
                end++;
            } else {
                break;
            }
        }
        try {
            return Double.parseDouble(objJson.substring(start, end));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String cleanQuotes(String s) {
        if (s == null) return "";
        return s.replace("\"", "").replace("'", "").trim();
    }
}

