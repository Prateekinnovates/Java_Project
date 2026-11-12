package com.expensetracker.storage;

import com.expensetracker.model.Account;

/**
 * OOP Concept: Interface & Abstraction
 * Defines the contract for persisting and retrieving user financial data and preferences.
 * By programming to an interface, the business logic remains decoupled from the specific
 * storage medium (e.g. browser localStorage, IndexedDB, or mock test storage).
 */
public interface StorageService {
    Account loadAccount();
    void saveAccount(Account account);
    void clear();
    String getTheme();
    void saveTheme(String theme);
    boolean hasStoredData();
}

