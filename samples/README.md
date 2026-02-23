# Job Processor Samples

This directory contains examples of `JobProcessor` implementations.

## SimpleProcessor

A basic processor that simulates work by sleeping for 3 seconds.

### Build and Package

1. Ensure the main `jobProc` project is installed in your local Maven repository:
   ```bash
   cd ../app
   mvn install -DskipTests
   ```
2. Build the samples project:
   ```bash
   mvn clean package
   ```
   This will produce `samples/target/jobProc-samples-1.0.0.jar`.

### Usage

1. Go to the Admin Panel in the Job Processor UI (`admin.html`).
2. Upload `samples/target/jobProc-samples-1.0.0.jar`.
3. Set the Class Name to: `com.sel2in.jobProc.samples.SimpleProcessor`.
4. Go to the Jobs page and schedule a new job using this processor.
