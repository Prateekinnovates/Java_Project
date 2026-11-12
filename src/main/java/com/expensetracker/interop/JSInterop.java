package com.expensetracker.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * OOP Concept: Interface & Bridge Pattern (TeaVM JSO Interop)
 * Bridges compiled Java code to native browser APIs and client-side JavaScript libraries
 * like Chart.js, file download blobs, DOM updates, and theme toggling.
 */
public class JSInterop {

    @JSFunctor
    public interface StringCallback extends JSObject {
        void invoke(String data);
    }

    @JSFunctor
    public interface VoidCallback extends JSObject {
        void invoke();
    }

    // Chart.js bridges
    @JSBody(params = { "canvasId", "monthsJson", "incomeJson", "expenseJson" },
            script = "if (window.ExpenseTrackerCharts) { window.ExpenseTrackerCharts.renderMonthlyBarChart(canvasId, JSON.parse(monthsJson), JSON.parse(incomeJson), JSON.parse(expenseJson)); }")
    public static native void renderMonthlyBarChart(String canvasId, String monthsJson, String incomeJson, String expenseJson);

    @JSBody(params = { "canvasId", "labelsJson", "amountsJson", "colorsJson" },
            script = "if (window.ExpenseTrackerCharts) { window.ExpenseTrackerCharts.renderCategoryDoughnut(canvasId, JSON.parse(labelsJson), JSON.parse(amountsJson), JSON.parse(colorsJson)); }")
    public static native void renderCategoryDoughnut(String canvasId, String labelsJson, String amountsJson, String colorsJson);

    @JSBody(params = { "canvasId", "datesJson", "amountsJson" },
            script = "if (window.ExpenseTrackerCharts) { window.ExpenseTrackerCharts.renderTrendLineChart(canvasId, JSON.parse(datesJson), JSON.parse(amountsJson)); }")
    public static native void renderTrendLineChart(String canvasId, String datesJson, String amountsJson);

    // CSV File Download
    @JSBody(params = { "filename", "content" },
            script = "if (window.ExpenseTrackerBridge) { window.ExpenseTrackerBridge.downloadFile(filename, content); }")
    public static native void downloadFile(String filename, String content);

    // Toast Notifications
    @JSBody(params = { "message", "type" },
            script = "if (window.ExpenseTrackerBridge) { window.ExpenseTrackerBridge.showToast(message, type); }")
    public static native void showToast(String message, String type);

    // Confirmation Dialog
    @JSBody(params = { "message" }, script = "return window.confirm(message);")
    public static native boolean confirm(String message);

    // Theme switching
    @JSBody(params = { "theme" },
            script = "if (window.ExpenseTrackerBridge) { window.ExpenseTrackerBridge.applyTheme(theme); }")
    public static native void applyTheme(String theme);

    // Registering global Java hooks to window with function wrapper
    @JSBody(params = { "name", "fn" },
            script = "window[name] = function(arg) { if (typeof fn === 'function') { return fn(arg); } else if (fn && fn.invoke) { return fn.invoke(arg); } };")
    public static native void registerGlobalFunction(String name, JSObject fn);
}

