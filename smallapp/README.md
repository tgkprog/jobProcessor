# SmallApp - Minimal Job Scheduler

A standalone, lightweight Spring Boot application designed for scheduling simple tasks with a modern, responsive web interface.

## 🚀 Quick Start

Use the integrated build script to stop, rebuild, and run the application:

```bash
./build c p r
```

- **UI URL**: [http://localhost:8081/small/index.html](http://localhost:8081/small/index.html)
- **API Prefix**: `/small/api`
- **Port**: 8081 (standalone) or 8087 (when embedded in Main App)

## 🛠 Management Scripts

- **`./build`**: Main build tool.
  - `c`: Clean project.
  - `p`: Rebuild and copy JAR to `bin/`.
  - `r`: Stop running instances and start the fresh JAR from `bin/`.
- **`./bkRun`**: Starts the application in the background using `sudo`.
- **`./kil`**: Robustly stops all running instances of the app using SIGTERM followed by SIGKILL.

## ✨ Key Features

- **Precise Scheduling**: Set jobs to run at any future date/time.
- **Configurable Sleep**: 
    - **Base Sleep**: Fixed duration in seconds.
    - **Random Sleep**: Optional additional delay (1ms to max seconds).
- **Status Dashboard**: Real-time tracking of states (**IDLE**, **SCHEDULED**, **WORKING**).
- **History Tracking**: Keeps a visual record of the last **5 scheduled jobs** (status, time, errors).
- **Auto-Refresh**: Optional 7-second auto-polling checkbox for real-time updates.

## 📁 Project Structure

- `bin/`: Contains the production-ready JAR.
- `logs/`: Application logs with automatic 6MB hourly rotation.
- `src/main/resources/static/`: Modern UI (Vanilla CSS/JS).
- `src/test/java/`: Includes a full **Selenium** UI test suite.

## ⚙️ Technical Details

- **Backend**: Java 17, Spring Boot 3.3.0.
- **Logging**: Configured via Logback for rolling rotation based on size (6MB) and time.
- **API Endpoints**:
  - `GET /api/status`: Returns current state and job history.
  - `POST /api/set`: Schedules a new job.
  - `POST /api/cancel`: Clears the current schedule.
