# Job Processor Samples

This directory contains example `JobProcessor` implementations that can be loaded dynamically by the Job Engine.

## Build and Package (all samples)

1. Ensure the main `jobProc` engine is installed in your local Maven repository:
   ```bash
   cd ../app
   mvn install -DskipTests
   ```
2. Build the samples JAR:
   ```bash
   cd ../samples
   mvn clean package
   ```
   Output: `samples/target/jobProc-samples-1.0.0.jar`

---

## SimpleProcessor

A minimal "hello-world" processor that simulates work by sleeping for 3 seconds.

**Class Name:** `com.sel2in.jobProc.samples.SimpleProcessor`

### Usage
1. Upload the samples JAR via the Admin Panel (`admin.html`).
2. Register the class name above.
3. Schedule a new job — no input files required.

---

## ExpenseTrackerProcessor

Parses CSV or pipe-separated (`|`) expense files, auto-categorizes each transaction by keyword matching, and generates a standalone **HTML report** with inline SVG bar chart + donut chart.

**Class Name:** `com.sel2in.jobProc.samples.ExpenseTrackerProcessor`

### Expected Input Format

CSV or pipe-delimited file with columns: `date, description, amount`

```csv
date,description,amount
2026-01-01,Home EMI payment,45000.00
2026-01-02,Grocery shopping,3200.50
```

Or pipe-separated:
```
date|description|amount
2026-02-01|Rent payment for apartment|22000.00
```

### Categories

Expenses are auto-classified into: **Housing**, **Food**, **Utilities**, **Transport**, **Healthcare**, **Education**, **Entertainment**, **Shopping**, and **Other**.

Keywords like `rent`, `home emi`, `restaurant`, `electricity`, `uber`, `pharmacy`, `netflix`, `amazon`, etc. are matched case-insensitively.

### Sample Data Files

Two ready-to-use sample files are included in `dataIn/`:
- `monthly_expenses_jan2026.csv` — 32 entries, comma-separated
- `expenses_feb2026_pipe.csv` — 28 entries, pipe-separated

### Usage
1. Upload the samples JAR and register the class via the Admin Panel.
2. Schedule a new job, attaching one or both sample CSV files as input.
3. The output report is saved to `outputFiles/<jobId>/expense_report.html`.
