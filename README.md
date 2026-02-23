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

## 🚦 Getting Started

### 1. Build the Engine
From the root directory:
```bash
cd app
mvn clean install
```

### 2. Run the Application
```bash
mvn spring-boot:run
```
The UI will be available at: [http://localhost:8080](http://localhost:8080)

### 3. Create a Sample Processor
To see the system in action, build the included sample:
```bash
cd samples
mvn clean package
```
Verify the output at `samples/target/jobProc-samples-1.0.0.jar`.

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
