package com.expensetracker.util;

import org.teavm.jso.JSBody;

/**
 * Utility for cross-platform number formatting in browser environment.
 * Uses native JS Number.toFixed for fast, accurate decimal formatting.
 */
public class NumberFormatUtil {

    @JSBody(params = { "val", "decimals" }, script = "return Number(val).toFixed(decimals);")
    public static native String toFixed(double val, int decimals);

    public static String formatCurrency(double val) {
        return "$" + toFixed(Math.abs(val), 2);
    }

    public static String formatAmount(double val) {
        return toFixed(val, 2);
    }

    public static String formatPercent(double val) {
        return toFixed(val, 1) + "%";
    }
}
