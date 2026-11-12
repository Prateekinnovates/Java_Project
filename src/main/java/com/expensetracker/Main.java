package com.expensetracker;

import com.expensetracker.service.ExpenseTrackerService;
import com.expensetracker.storage.LocalStorageService;
import com.expensetracker.storage.StorageService;
import com.expensetracker.ui.ExpenseTrackerUI;

/**
 * OOP Concept: Application Entry Point & Dependency Injection
 * Boots the application inside the browser environment compiled via TeaVM.
 * Instantiates the storage provider, business service, and presentation controller.
 */
public class Main {
    public static void main(String[] args) {
        StorageService storageService = new LocalStorageService();
        ExpenseTrackerService service = new ExpenseTrackerService(storageService);
        ExpenseTrackerUI ui = new ExpenseTrackerUI(service);
        ui.init();
    }
}
