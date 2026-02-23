# Job Processor Engine

A powerful, dynamic Job Execution and Management system built with Java 11, Spring Boot, and Quartz. This engine allows you to load and run "Job Processors" provided as external JAR files at runtime without restarting the main service.

## 🚀 Key Features

-   **Dynamic JAR Loading**: Upload and execute external Java code. Supports versioning and on-the-fly class loading.
-   **Security & Validation**: Optional SHA-256 checksum verification for uploaded JARs to ensure code integrity.
-   **Advanced Scheduling**: Built-in Quartz scheduler for managing job execution with precise timing (cron-like precision).
-   **Job Monitoring Dashboard**: A modern, real-time web interface to track job status, execution history, and performance metrics.
-   **File Management**: Automatic handling of input data files associated with specific job runs.
-   **Resource Management**: Controlled execution using configurable thread pools and instance-based distribution.
-   **Timeout Protection**: Automated interruption of jobs that exceed their estimated runtime (default 150% threshold).

## 🛠 Technical Stack

-   **Backend**: Java 11, Spring Boot 2.7.18
-   **Database**: H2 (Embedded / In-memory)
-   **ORM**: Spring Data JPA / Hibernate
-   **Scheduling**: Quartz / Spring Scheduler
-   **Frontend**: Vanilla HTML5/CSS3/JS (Modern Refactored UI)

## 📁 Project Structure

```text
.
├── app/              # Main Spring Boot Engine
│   ├── src/          # Source code & static web resources
│   ├── inputFiles/   # Managed storage for job inputs
│   └── processors/   # Storage for uploaded JAR processors
├── samples/          # Reference implementations of JobProcessors
└── data/             # Persistent data storage
```

## 🚦 Getting Started (Fresh System)

### Prerequisites
-   Java 11+ (JDK)
-   Maven 3.6+

### Step 1: Build & Install the Engine
```bash
cd app
mvn clean install
```
This compiles the engine, runs tests, and installs the core JAR to your local Maven repo (required by samples).

### Step 2: Build the Sample Processors
```bash
cd ../samples
mvn clean package
```
Output: `samples/target/jobProc-samples-1.0.0.jar`

### Step 3: Deploy the Samples JAR
Copy the built JAR into the engine's `processors/` directory:
```bash
cp samples/target/jobProc-samples-1.0.0.jar app/processors/
```

### Step 4: Run the Engine
```bash
cd app
java -jar target/jobProc-1.0.0-fat.jar
```
Or via Maven:
```bash
cd app
mvn spring-boot:run
```
The UI will be available at: [http://localhost:8080](http://localhost:8080)

### Step 5: Register Processors via Admin UI
1.  Open `/admin.html` (login: `admin` / `admin`).
2.  Upload the samples JAR, or register processors manually with class names:
    -   `com.sel2in.jobProc.samples.SimpleProcessor`
    -   `com.sel2in.jobProc.samples.ExpenseTrackerProcessor`

### Quick One-Liner (build everything)
```bash
cd app && mvn clean install && cd ../samples && mvn clean package && cp target/jobProc-samples-1.0.0.jar ../app/processors/ && cd ../app && java -jar target/jobProc-1.0.0-fat.jar
```

### ⚠️ Re-deploying Updated JARs
If you rebuild a samples JAR after the engine is already running, use the **Admin Panel → Upload JAR** feature instead of a manual file copy. This ensures the engine evicts its cached ClassLoader and picks up the new classes.

## 🖥 Admin Panel & Security

The administrative functions (`admin.html` and `/api/admin/**`) are protected via Basic Authentication.

-   **Default URL**: `/admin.html`
-   **Credentials**: `admin` / `admin`

In the Admin Panel, you can:
-   Upload new Processor JARs.
-   Register classes that implement the `JobProcessor` interface.
-   Monitor system-wide statistics.

## 🏗 Developing a Job Processor

Your external JAR must include a class that implements the `com.sel2in.jobProc.processor.JobProcessor` interface.

```java
public interface JobProcessor {
    boolean acceptJob(JobContext context);
    JobResult processJob(JobContext context);
}
```

Refer to the `samples/` directory for a complete implementation example (`SimpleProcessor`).

---
Developed with ❤️ by Tushar Kapila
