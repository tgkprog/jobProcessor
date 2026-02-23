# Job Processor: TODO & Don't

## ✅ DONE

### Core Infrastructure
- [x] Spring Boot app with H2 embedded database (`ddl-auto: update`)
- [x] Pre-Spring bootstrap: auto-creates working directories
- [x] `DataSeeder`: auto-populates `AppParams` on first run
- [x] `JobProcessor` interface (`reviewJob`, `processJob`)
- [x] `ProcessorLoader`: dynamic JAR loading with `URLClassLoader` + cache + `evictCache`
- [x] `JobEngine`: thread pool loaded from `AppParams` DB, runtime resizing, job tracking, cancel
- [x] H2 Web Console at `/h2`

### JPA Entities (all done)
- [x] `AppParam`
- [x] `ProcessorDefinition`
- [x] `JobRecord` (InputData table) with `scheduledRunTime`
- [x] `InputDataParam`
- [x] `InputDataFile`
- [x] `OutputDataRecord`
- [x] `OutputDataParam`
- [x] `OutputDataFile`
- [x] `JobError`

### Repositories
- [x] `AppParamRepository`
- [x] `ProcessorRepository`
- [x] `JobRepository`

### Scheduling (Quartz)
- [x] `ScheduledJobTrigger`: Quartz Job fires at scheduled time
- [x] `JobExecutionService`: loads job from DB, resolves JAR, runs via JobEngine, updates status
- [x] Minimum 30s delay enforced (cron scheduler, not immediate)
- [x] `GET /api/job/run?jobId=N` manual trigger endpoint

### REST Controllers
- [x] `ProcessorController`: CRUD processors
- [x] `JobController`: schedule with delay, status, manual run
- [x] `AdminController`: AppParams CRUD, engine status, thread pool resize, job cancel

### Admin UI
- [x] `jobs.html`: schedule with delay picker, job table, Run Now, auto-refresh 5s
- [x] `jobsProcs.html`: processor management
- [x] File upload in `jobs.html` for input files
- [x] Job cancellation button in UI
- [x] Thread pool / AppParams editor page in UI

### Dynamic Loading
- [x] Upload JAR via REST endpoint (multipart file upload)
- [x] JAR checksum validation before loading (SHA-256)

### Production
- [x] Support `--dbReset` flag to wipe and re-seed DB
- [x] MySQL/PostgreSQL datasource profile template
- [x] Security: Basic HTTP authentication for admin APIs
- [x] `samples/` project: example JobProcessor implementations

### Testing
- [x] 15 JUnit integration tests (`src/test/java`) with real H2 in-memory DB
- [x] `test.sh` shell-based smoke tests against running fat JAR

---

## 📋 TODO 


- [x] Advanced dashboard with charts for job execution history.
        We store every schduled job, status, if it was canceled before run, or waiting (to be run) the time its meant to be run (earliest_run_time)
        if it was run, then the time it was run (start_run_time) and the time it finished (end_run_time); main eror code, eror reason (forget about the list of errors for now)

---

References:
app/docs/req.html
other files in app/docs/
        

---

## 🚫 DON'T (Anti-Patterns to Avoid)

### Architecture
- **Don't** hardcode JAR paths. Resolve via DB or config.
- **Don't** reuse a `JobProcessor` instance across concurrent jobs if it holds state.
- **Don't** let Job Processors write directly to the DB. They return `OutputData`.

### Performance
- **Don't** block the HTTP thread waiting for a job. Use `CompletableFuture`.
- **Don't** keep unbounded logs in memory. Persist errors to `JobError` table.
- **Don't** reload the same JAR ClassLoader unless `evictCache` is called.

### Security
- **Don't** allow processors to call `System.exit()`.
- **Don't** run the engine with root privileges.
- **Don't** expose raw DB IDs externally if the system goes public.

---
Created by Tushar Kapila
