# Job Processor: Implementation Best Practices (Todo & Don't)

## 📋 TODO (Core Implementation Path)

### 1. Database & Persistence (JPA)
- [ ] Implement all Entities: `InputData`, `InputDataParam`, `InputDataFile`, `OutputData`, `OutputDataParam`, `OutputDataFile`, `JobError`.
- [ ] Create Repositories for all entities.
- [ ] Implement `AppParamsService` to prioritize DB values over `application.yaml`.

### 2. Job Engine Enhancements
- [ ] Implement proper thread pool resizing logic (handling shutdown/migration of running tasks).
- [ ] Add `JobTracker` to monitor currently running jobs in memory (for UI view).
- [ ] Implement `Cancel Job` logic using `Future.cancel(true)`.

### 3. Dynamic Loading Safety
- [ ] Use separate `ClassLoader` for each processor version to allow hot-reloading.
- [ ] Validate JAR integrity and class signature before instantiation.
- [ ] Implement a `Sandbox` environment or Security Manager for untrusted JARs.

### 4. REST APIs
- [ ] Implement `/api/job/schedule` to consume `FormData` (files + params).
- [ ] Implement `/api/appParam` for runtime configuration.
- [ ] Add pagination to `/api/job/status` for performance.

---

## 🚫 DON'T (Anti-Patterns to Avoid)

### 1. Architecturally
- **Don't** hardcode JAR paths. Always resolve via base directory from config or DB.
- **Don't** use the same instance of `IJobProcessor` for multiple concurrent jobs if state is maintained. (Factory pattern or new instance per job).
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
