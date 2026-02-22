# Job Processor: Implementation Best Practices (Todo & Don't)

## 📋 TODO (Core Implementation Path)

### 1. Database & Persistence (JPA)
- [x] Implement core Entities: `ProcessorDefinition`, `AppParam`, `JobRecord`.
- [ ] Implement support Entities: `InputDataParam`, `OutputDataParam`, `JobError`.
- [x] Create Repositories for core entities.
- [ ] Implement `AppParams` logic in `JobEngine` to prioritize DB values over `application.yaml`.

### 2. Job Engine Enhancements
- [x] Basic thread pool execution with `CompletableFuture`.
- [ ] Implement proper thread pool resizing logic (graceful shutdown of active tasks).
- [ ] Implement `JobTracker` in-memory store for real-time monitoring of active threads.
- [ ] Implement `Cancel Job` logic using task interruption.

### 3. Dynamic Loading & Safety
- [x] Functional `ProcessorLoader` with `URLClassLoader` and caching.
- [ ] Implement `evictCache` logic when a JAR is re-uploaded or removed.
- [ ] Add JAR hashing/checksum validation before loading.

### 4. REST APIs & UI
- [x] Implement `/api/job/listAll`, `add`, `remove`.
- [x] Implement `/api/job/schedule` (basic) and `/api/job/status`.
- [x] Implement Admin UI (`jobs.html`, `jobsProcs.html`) using real API calls.
- [ ] Implement Multipart file upload for JARs and Input Files.

---

## 🚫 DON'T (Anti-Patterns to Avoid)

### 1. Architecturally
- **Don't** hardcode JAR paths. Always resolve via base directory from config or DB.
- **Don't** use the same instance of `JobProcessor` for multiple concurrent jobs if state is maintained. (Factory pattern or new instance per job).
- **Don't** let Job Processors write directly to the root DB. They should return `OutputData` and let the Engine handle persistence.

### 2. Performance
- **Don't** block the main server thread waiting for a job. Use `Future` or `CompletableFuture`.
- **Don't** keep infinite logs in memory. Persist to DB and clear memory.
- **Don't** load the same JAR ClassLoader multiple times unless reloading is explicitly requested (performance overhead).

### 3. Security
- **Don't** allow Job Processors to execute `System.exit()`.
- **Don't** expose internal DB IDs directly in the URL if possible (use UUIDs for Job Tracking).
- **Don't** run the engine with ROOT privileges; isolated users are safer for dynamic JAR execution.

---
Created by Tushar Kapila
