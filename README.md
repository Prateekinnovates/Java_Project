# ExpensePulse — Client-Side Java OOP Expense Tracker

A modern, responsive, frontend-only Expense Tracker and Financial Reporting web application built in **Java 17** using **Object-Oriented Programming (OOP)** principles and compiled to run natively in the browser via **TeaVM**.

> **No Backend • No Server • No Database • 100% Client-Side Java**  
> All data persists across page reloads in the browser's `localStorage`. Deployable instantly as a static website on **Vercel**, GitHub Pages, or Netlify.

---

## 🌟 Key Features

1. **Transaction Management (Income & Expense)**
   - Add, edit, and delete transactions with full validation (amount, date, category, description/note).
   - Polymorphic handling of cash inflows (+) and cash outflows (-).
2. **Dynamic Financial Dashboard**
   - Live KPI cards: **Total Current Balance**, **Total Income**, **Total Expense**, and **This Month's Spending**.
   - Recent Transactions feed with category icons and status badges.
   - Monthly category budget tracking cards with real-time visual progress bars and warnings.
3. **Advanced Filtering, Search & Sorting**
   - Real-time search by description, category, amount, or date.
   - Filter by Type (`All`, `Income`, `Expense`), Category (`Food`, `Travel`, `Bills`, etc.), and Custom Date Ranges (`From` / `To`).
   - Sort by Date (newest/oldest) or Amount (highest/lowest) powered by Java Streams.
4. **Interactive Reports & Visual Analytics**
   - **Monthly Summary**: Grouped bar chart comparing income vs. expense per month using Chart.js.
   - **Category Breakdown**: Proportional doughnut chart visualizing spending distributions.
   - **Spending Trend**: Chronological line chart tracking daily/monthly outflow trends.
   - **Top 3 Spending Categories**: Automated rank and percentage insight cards.
   - **Category Budget Compliance Table**: Visual indicator whether spending is within limits or exceeded.
5. **Monthly Category Budgets & Warnings**
   - Set monthly spending limits per category (e.g. Food, Shopping, Travel).
   - Real-time visual alerts (progress bars turn orange at 80%+ usage and red when exceeded).
6. **CSV Export & Import**
   - Export the full transaction ledger to RFC 4180 compliant CSV files with one click.
   - Import transactions back from CSV files, automatically validating and parsing lines.
7. **Strict Input Validation & Custom Exceptions**
   - Inline form error indicators powered by a dedicated Java Exception hierarchy.
8. **Browser LocalStorage Persistence**
   - All transactions and budget limits persist across page refreshes.
   - Single-click **"Clear All Data"** with safety confirmation dialog and **"Reset Sample Data"** to restore demo data.
9. **Modern Responsive Design & Light/Dark Mode**
   - Polished dashboard with smooth theme transitions and persistent theme preferences in `localStorage`.

---

## 🎓 Object-Oriented Programming (OOP) Design

This project is structured specifically to showcase clean, enterprise-grade OOP principles in Java 17 for academic grading and viva examinations:

```
                                  ┌───────────────────────────┐
                                  │      <<interface>>        │
                                  │      StorageService       │
                                  └─────────────▲─────────────┘
                                                │ implements
                                  ┌─────────────┴─────────────┐
                                  │    LocalStorageService    │
                                  └───────────────────────────┘

         ┌──────────────────────────────────────────────────────────────────┐
         │                               Account                            │
         │  - transactions: List<Transaction>                               │
         │  - monthlyBudgets: Map<Category, Double>                         │
         │  + calculateBalance(): double  (Polymorphic summation)          │
         │  + calculateTotalIncome(): double (Stream filter)                │
         │  + calculateTotalExpense(): double (Stream filter)               │
         └────────────────────────────────┬─────────────────────────────────┘
                                          │ 1..* aggregates
                                          ▼
                             ┌─────────────────────────┐
                             │    <<abstract class>>   │
                             │       Transaction       │
                             ├─────────────────────────┤
                             │ - id: String            │
                             │ - amount: double        │
                             │ - date: String          │
                             │ - description: String   │
                             │ - category: Category    │
                             ├─────────────────────────┤
                             │ + getSignedAmount()*    │
                             │ + getType()*            │
                             └────────────▲────────────┘
                                          │
                         ┌────────────────┴────────────────┐
                         │ extends                         │ extends
           ┌─────────────┴─────────────┐     ┌─────────────┴─────────────┐
           │          Income           │     │          Expense          │
           ├───────────────────────────┤     ├───────────────────────────┤
           │ + getSignedAmount(): +amt │     │ + getSignedAmount(): -amt │
           │ + getType(): "INCOME"     │     │ + getType(): "EXPENSE"    │
           └───────────────────────────┘     └───────────────────────────┘

                                  ┌───────────────────────────┐
                                  │      <<interface>>        │
                                  │    ReportGenerator<T>     │
                                  └─────────────▲─────────────┘
                                                │ implements
                   ┌────────────────────────────┼────────────────────────────┐
     ┌─────────────┴─────────────┐┌─────────────┴─────────────┐┌─────────────┴─────────────┐
     │       MonthlyReport       ││      CategoryReport       ││   IncomeVsExpenseReport   │
     └───────────────────────────┘└───────────────────────────┘└───────────────────────────┘
```

### 1. Abstraction
- **`Transaction`**: An abstract base class encapsulating common transaction attributes (`id`, `amount`, `date`, `description`, `category`). It defines abstract contractual methods `getSignedAmount()` and `getType()` that must be implemented by subclasses. It cannot be directly instantiated.

### 2. Inheritance
- **`Income`** and **`Expense`**: Both inherit from `Transaction`, automatically acquiring the fields, constructor validation, getters, and CSV formatting methods without duplicating code.

### 3. Polymorphism
- **Dynamic Method Dispatch**: `Income` overrides `getSignedAmount()` to return `+amount`, while `Expense` overrides it to return `-amount`.
- **Polymorphic Account Balance**: `Account.calculateBalance()` iterates over `List<Transaction>` and invokes `tx.getSignedAmount()`. The runtime dynamically executes the appropriate subclass logic without knowing whether an entry is an `Income` or an `Expense`.
- **Strategy Pattern via Interfaces**: `ReportGenerator<T>` is polymorphically implemented by `MonthlyReport`, `CategoryReport`, and `IncomeVsExpenseReport`.

### 4. Encapsulation
- **`Account`**: Encapsulates internal financial state (`List<Transaction>` and `Map<Category, Double>`). State is accessed via `getTransactions()` returning `Collections.unmodifiableList()` to prevent unauthorized external mutation.
- **Strict Mutator Validation**: Setters in `Transaction` strictly validate amounts against non-positive numbers (`InvalidAmountException`), check date formats, and reject blank descriptions (`ValidationException`).

### 5. Interfaces
- **`StorageService`**: Defines the contract for persistence (`loadAccount()`, `saveAccount()`, `clear()`, `getTheme()`). Implemented by `LocalStorageService` using TeaVM JSO to interact with browser `window.localStorage`. Decouples business logic from browser-specific APIs.
- **`ReportGenerator<T>`**: Defines the contract for generating analytics from an `Account`.

### 6. Rich Enumerations
- **`Category`**: Type-safe enum specifying metadata (`displayName`, `icon`, `CategoryType`, and chart `color`).

### 7. Custom Exception Hierarchy
- **`ExpenseTrackerException`**: Base unchecked business exception.
- **`InvalidAmountException`**: Thrown on invalid, non-numeric, or non-positive monetary input.
- **`TransactionNotFoundException`**: Thrown when targeting an unrecorded transaction ID.
- **`ValidationException`**: Thrown on malformed input (empty notes, invalid date syntax).

### 8. Java Collections & Streams
- Used extensively (`List`, `Map`, `TreeMap`, `EnumMap`, `Collectors.groupingBy`, `Collectors.summingDouble`, `filter`, `sorted`, `limit`) for grouping transactions by month/category and computing insights.

---

## 📁 Project Structure

```
java-project/
├── .tools/                               # Portable OpenJDK 17 and Apache Maven 3.9
├── dist/                                 # Compiled production static site (served by Vercel)
│   ├── css/
│   │   └── style.css                     # Responsive CSS (Light & Dark theme)
│   ├── js/
│   │   └── app.js                        # Compiled Java bytecode -> JavaScript (TeaVM)
│   └── index.html                        # Application single-page layout & Chart.js interop
├── src/
│   ├── main/
│   │   ├── java/com/expensetracker/
│   │   │   ├── exception/
│   │   │   │   ├── ExpenseTrackerException.java
│   │   │   │   ├── InvalidAmountException.java
│   │   │   │   ├── TransactionNotFoundException.java
│   │   │   │   └── ValidationException.java
│   │   │   ├── interop/
│   │   │   │   └── JSInterop.java        # TeaVM JSO Native JavaScript Bridges
│   │   │   ├── model/
│   │   │   │   ├── Account.java          # Ledger & Encapsulation
│   │   │   │   ├── Category.java         # Enum with icons and color metadata
│   │   │   │   ├── CategoryBudget.java   # Budget tracking model
│   │   │   │   ├── CategoryType.java     # Classification enum
│   │   │   │   ├── Expense.java          # Subclass (Polymorphism)
│   │   │   │   ├── Income.java           # Subclass (Polymorphism)
│   │   │   │   └── Transaction.java      # Abstract Base Class
│   │   │   ├── report/
│   │   │   │   ├── CategoryReport.java   # Strategy implementation
│   │   │   │   ├── CategoryReportResult.java
│   │   │   │   ├── IncomeVsExpenseReport.java # Strategy implementation
│   │   │   │   ├── IncomeVsExpenseReportResult.java
│   │   │   │   ├── MonthlyReport.java    # Strategy implementation
│   │   │   │   ├── MonthlyReportResult.java
│   │   │   │   └── ReportGenerator.java  # Interface
│   │   │   ├── service/
│   │   │   │   └── ExpenseTrackerService.java # Facade / Business Service
│   │   │   ├── storage/
│   │   │   │   ├── LocalStorageService.java   # Browser LocalStorage implementation
│   │   │   │   └── StorageService.java        # Interface
│   │   │   ├── ui/
│   │   │   │   └── ExpenseTrackerUI.java      # DOM View Controller & Presentation
│   │   │   └── Main.java                      # Client-side bootstrap entry point
│   │   └── webapp/
│   │       ├── css/style.css
│   │       └── index.html
├── mvnw.cmd                              # Maven Wrapper for Windows
├── build.bat                             # One-click build script
├── pom.xml                               # Maven build configuration with TeaVM plugin
├── vercel.json                           # Vercel static deployment configuration
└── README.md                             # Project documentation & Viva guide
```

---

## 🛠️ How to Build with Maven

### Prerequisites
- **Java 17** (or portable JDK included in `.tools/`)
- **Maven 3.8+** (or portable Maven included in `.tools/`)

### Single Build Command
Run the standard Maven packaging command from the project root:

```bash
mvn clean package
```

*(On Windows, you can also simply double-click `build.bat` or run `.\mvnw.cmd clean package`)*

### What the build does:
1. Cleans target and previous `/dist` contents.
2. Copies static web assets from `src/main/webapp/` (`index.html`, `css/style.css`) into `/dist/`.
3. Compiles all Java 17 source files using `javac`.
4. Executes the **TeaVM Maven Plugin** (`org.teavm:teavm-maven-plugin:0.9.2`) to translate the compiled Java bytecode into client-side JavaScript (`/dist/js/app.js`).
5. Generates the self-contained production `/dist` directory.

---

## 🚀 How to Deploy to Vercel

Because Vercel's standard serverless build containers do not reliably bundle Java 17 and TeaVM, this repository commits the prebuilt, fully static `/dist` directory and includes `vercel.json`.

### `vercel.json` Configuration
```json
{
  "version": 2,
  "outputDirectory": "dist"
}
```

### Steps to Deploy:
1. **Push your code to GitHub / GitLab / Bitbucket**:
   ```bash
   git add .
   git commit -m "Build and release Expense Tracker"
   git push origin main
   ```
2. **Import to Vercel**:
   - Go to [vercel.com/new](https://vercel.com/new).
   - Select your Git repository.
   - Leave the **Build Command** blank (or set to `None`).
   - The **Output Directory** is automatically detected from `vercel.json` as `dist`.
   - Click **Deploy**.
3. **Alternatively, deploy directly using Vercel CLI**:
   ```bash
   npm i -g vercel
   vercel --prod
   ```

Your static site will be live worldwide on Vercel's Edge CDN with zero server setup or recurring costs.

---

## 🧪 Testing Locally

To run the static site locally using Node.js:

```bash
node serve.js
```

Then open your browser at:
```
http://localhost:3000
```

---

## 📄 License
This project is open-source under the MIT License.

