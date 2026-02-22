# Job Processor: TODO & Don't

## ✅ DONE

### Core Infrastructure
- [x] Spring Boot app with H2 embedded database (`ddl-auto: update`)
- [x] Pre-Spring bootstrap: auto-creates working directories (`data/`, `processors/`, `inputFiles/`, `outputFiles/`)
- [x] `DataSeeder`: auto-populates `AppParams` on first run
- [x] `JobProcessor` interface (`reviewJob`, `processJob`)
- [x] `ProcessorLoader`: dynamic JAR loading with `URLClassLoader` + cache
- [x] `JobEngine`: thread pool execution with timeout (150% of estimate)
- [x] JPA Entities: `AppParam`, `ProcessorDefinition`, `JobRecord`
- [x] Repositories: `AppParamRepository`, `ProcessorRepository`, `JobRepository`
- [x] REST Controllers: `ProcessorController`, `JobController`
- [x] Admin UI: `jobs.html`, `jobsProcs.html` with real API calls
- [x] H2 Web Console at `/h2`

### Verified Working
- [x] `GET /api/job/listAll` — list all registered processors
- [x] `POST /api/job/add` — register a new processor
- [x] `DELETE /api/job/remove/{className}` — remove a processor
- [x] `POST /api/job/schedule` — schedule a job (persists to DB)
- [x] `GET /api/job/status` — list all jobs with status

---

## 📋 TODO

### Database & Persistence
- [ ] Implement remaining entities: `InputDataParam`, `InputDataFile`, `OutputDataParam`, `OutputDataFile`, `JobError`
- [ ] Load `numberOfThreads` from `AppParams` DB at startup instead of hardcoding
- [ ] Support `--dbReset` flag to wipe and re-seed DB

### Job Engine
- [ ] Implement proper thread pool resizing at runtime
- [ ] `JobTracker`: in-memory map of active job futures for monitoring
- [ ] Cancel job logic via `Future.cancel(true)`
- [ ] Wire `executeAsync` to the `/api/job/schedule` endpoint (currently only persists record)
- [ ] Record job start/end times and status changes in DB after execution

### Dynamic Loading
- [ ] `evictCache` when a JAR is re-uploaded or deleted
- [ ] JAR checksum validation before loading
- [ ] Upload JAR via REST endpoint

### Admin UI
- [ ] File upload support in `jobs.html` for input files
- [ ] Live job status auto-refresh indicators
- [ ] Job cancellation button
- [ ] Thread pool size control panel
- [ ] AppParams editor page

---

## 🚫 DON'T (Anti-Patterns to Avoid)

### Architecture
- **Don't** hardcode JAR paths. Resolve via DB or config.
- **Don't** reuse a `JobProcessor` instance across concurrent jobs if it holds state. Create a new instance per job via `ProcessorLoader.load()`.
- **Don't** let Job Processors write directly to the DB. They return `OutputData`; the Engine persists it.

### Performance
- **Don't** block the HTTP thread waiting for a job. Use `CompletableFuture`.
- **Don't** keep unbounded logs in memory. Persist errors to `JobError` table.
- **Don't** reload the same JAR ClassLoader unless explicitly requested.

### Security
- **Don't** allow processors to call `System.exit()`.
- **Don't** run the engine with root privileges.
- **Don't** expose raw DB IDs externally if the system goes public.

---
Created by Tushar Kapila
