# Job Processor

A robust Job Engine and Job Processor system built with Java 11 and Spring Boot.

## Overview

This project implements a dynamic job execution system where "Job Processors" are external JAR files loaded at runtime. The "Job Engine" manages execution, thread pools, and timeouts.

### Key Features

- **Dynamic Loading**: Load and reload Job Processors from external JARs without restarting the engine.
- **Resource Management**: Configurable thread pool and instance-based job distribution.
- **Monitoring**: Real-time tracking of job status, execution time, and error logs.
- **Timeout Handling**: Automated interruption of jobs exceeding 150% of their estimated time.
- **Web UI**: Embedded HTML interface for managing the system and viewing jobs.

## Technical Stack

- **Core**: Java 11
- **Framework**: Spring Boot 2.7.x
- **Batch Processing**: Spring Batch
- **Build Tool**: Maven
- **Scheduling**: Spring Scheduler

## Getting Started

### Prerequisites

- Java 11
- Maven

### Installation

1. Clone the repository:
   ```bash
   git clone git@github.com:tgkprog/jobProcessor.git
   ```
2. Build the project:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Architecture

The system consists of several main components:
- **Job Engine**: Orchestrates job submission and monitoring.
- **Job Processor Interface**: Standard interface (`acceptJob`, `processJob`) for external implementations.
- **Management Controller**: REST/Web interface for runtime configuration.
- **Thread Pool Executor**: Manages concurrent execution of jobs.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Created by Tushar Kapila
