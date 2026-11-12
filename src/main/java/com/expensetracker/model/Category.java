package com.expensetracker.model;

/**
 * OOP Concept: Enumeration & Encapsulation
 * Represents standardized transaction categories with associated metadata,
 * color codes for charts, icons, and classification.
 */
public enum Category {
    // Income Categories
    SALARY("Salary", "💰", CategoryType.INCOME, "#10b981"),
    FREELANCE("Freelance", "💻", CategoryType.INCOME, "#06b6d4"),
    INVESTMENT("Investment", "📈", CategoryType.INCOME, "#3b82f6"),

    // Expense Categories
    FOOD("Food & Dining", "🍔", CategoryType.EXPENSE, "#f59e0b"),
    TRAVEL("Travel & Commute", "✈️", CategoryType.EXPENSE, "#8b5cf6"),
    BILLS("Bills & Utilities", "💡", CategoryType.EXPENSE, "#ec4899"),
    SHOPPING("Shopping", "🛍️", CategoryType.EXPENSE, "#ef4444"),
    HEALTH("Health & Medical", "💊", CategoryType.EXPENSE, "#14b8a6"),
    EDUCATION("Education", "📚", CategoryType.EXPENSE, "#6366f1"),
    ENTERTAINMENT("Entertainment", "🎬", CategoryType.EXPENSE, "#f97316"),

    // General / Miscellaneous
    OTHER("Other", "📦", CategoryType.BOTH, "#64748b");

    private final String displayName;
    private final String icon;
    private final CategoryType type;
    private final String color;

    Category(String displayName, String icon, CategoryType type, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.type = type;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public CategoryType getType() {
        return type;
    }

    public String getColor() {
        return color;
    }

    public String getFormattedName() {
        return icon + " " + displayName;
    }

    public static Category fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OTHER;
        }
        String clean = value.trim().toUpperCase().replace("&", "").replace(" ", "_");
        for (Category cat : values()) {
            if (cat.name().equalsIgnoreCase(clean) || cat.displayName.equalsIgnoreCase(value.trim())) {
                return cat;
            }
        }
        // Fallback checks
        if (clean.contains("FOOD") || clean.contains("DIN")) return FOOD;
        if (clean.contains("TRAVEL") || clean.contains("TRIP")) return TRAVEL;
        if (clean.contains("BILL") || clean.contains("UTIL")) return BILLS;
        if (clean.contains("SHOP")) return SHOPPING;
        if (clean.contains("HEALTH") || clean.contains("MED")) return HEALTH;
        if (clean.contains("SALAR")) return SALARY;
        if (clean.contains("INVEST")) return INVESTMENT;
        if (clean.contains("FREELANCE")) return FREELANCE;
        if (clean.contains("EDU")) return EDUCATION;
        if (clean.contains("ENTERTAIN")) return ENTERTAINMENT;
        return OTHER;
    }
}

