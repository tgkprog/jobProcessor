package com.sel2in.jobProc.service;

import com.sel2in.jobProc.entity.AppParam;
import com.sel2in.jobProc.processor.JobProcessor;
import com.sel2in.jobProc.processor.InputData;
import com.sel2in.jobProc.processor.JobEstimate;
import com.sel2in.jobProc.processor.OutputData;
import com.sel2in.jobProc.repo.AppParamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Service
public class JobEngine {

    private final ProcessorLoader processorLoader;
    private final AppParamRepository appParamRepository;

    /** Tracks active job futures by jobId for monitoring and cancellation */
    private final Map<Long, CompletableFuture<OutputData>> activeJobs = new ConcurrentHashMap<>();
    private ThreadPoolExecutor executorService;

    public JobEngine(ProcessorLoader processorLoader, AppParamRepository appParamRepository) {
        this.processorLoader = processorLoader;
        this.appParamRepository = appParamRepository;
    }

    @PostConstruct
    public void startup() {
        int poolSize = loadThreadPoolSize();
        log.info("Starting Job Engine with thread pool size: {}", poolSize);
        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Loads thread pool size from AppParams DB, falls back to 5.
     */
    private int loadThreadPoolSize() {
        try {
            Optional<AppParam> param = appParamRepository.findById("numberOfThreads");
            if (param.isPresent()) {
                int size = Integer.parseInt(param.get().getValue());
                log.info("Thread pool size from DB: {}", size);
                return Math.max(1, Math.min(size, 50)); // clamp 1-50
            }
        } catch (Exception e) {
            log.warn("Could not load numberOfThreads from DB: {}", e.getMessage());
        }
        return 5;
    }

    /**
     * Resize the thread pool at runtime.
     */
    public void resizePool(int newSize) {
        newSize = Math.max(1, Math.min(newSize, 50));
        log.info("Resizing thread pool: {} -> {}", executorService.getCorePoolSize(), newSize);
        executorService.setCorePoolSize(newSize);
        executorService.setMaximumPoolSize(newSize);
    }

    public int getPoolSize() {
        return executorService.getCorePoolSize();
    }

    public int getActiveCount() {
        return executorService.getActiveCount();
    }

    /**
     * Submits a job for asynchronous execution and tracks it.
     * Returns a future that includes timeout information.
     */
    public CompletableFuture<OutputData> executeAsync(InputData inputData, String jarPath, String checksum) {
        Long jobId = inputData.getInputDataId();
        CompletableFuture<OutputData> future = CompletableFuture.supplyAsync(
                () -> execute(inputData, jarPath, checksum), executorService);
        if (jobId != null) {
            activeJobs.put(jobId, future);
            future.whenComplete((result, ex) -> activeJobs.remove(jobId));
        }
        return future;
    }

    /**
     * Calculate timeout for a job based on processor estimate.
     * @return timeout info string for logging/notes
     */
    public String calculateTimeout(InputData inputData, String jarPath, String checksum) {
        try {
            String className = inputData.getProcessorClassName();
            JobProcessor processor = processorLoader.load(jarPath, className, checksum);
            JobEstimate estimate = processor.reviewJob(inputData);
            long estimateMs = estimate.getMaxTimeToProcessMillis();
            long timeoutMs = (long) (estimateMs * 1.5);
            return String.format("Estimate: %dms, Timeout: %dms (150%%)", estimateMs, timeoutMs);
        } catch (Exception e) {
            log.warn("Could not calculate timeout for job {}: {}", inputData.getJobName(), e.getMessage());
            return "Timeout calculation failed";
        }
    }

    /**
     * Cancel a running job by its DB ID.
     * @return true if cancellation was requested
     */
    public boolean cancelJob(Long jobId) {
        CompletableFuture<OutputData> future = activeJobs.get(jobId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            log.info("Cancel requested for job {}: {}", jobId, cancelled);
            activeJobs.remove(jobId);
            return cancelled;
        }
        log.warn("Job {} not found in active jobs or already completed", jobId);
        return false;
    }

    /**
     * Returns the set of currently tracked active job IDs.
     */
    public java.util.Set<Long> getActiveJobIds() {
        return activeJobs.keySet();
    }

    private OutputData execute(InputData inputData, String jarPath, String checksum) {
        String className = inputData.getProcessorClassName();
        log.info("Executing job: {} using processor: {}", inputData.getJobName(), className);

        try {
            JobProcessor processor = processorLoader.load(jarPath, className, checksum);
            
            // 1. Review the job to get estimates
            JobEstimate estimate = processor.reviewJob(inputData);
            long timeoutMillis = (long) (estimate.getMaxTimeToProcessMillis() * 1.5);
            log.debug("Job timeout calculated as: {}ms", timeoutMillis);

            // 2. Process with timeout
            return CompletableFuture.supplyAsync(() -> processor.processJob(inputData))
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            log.error("Job timed out: {}", inputData.getJobName());
            return createErrorResult("TIMED_OUT", "Execution exceeded allowed time: " + className);
        } catch (CancellationException e) {
            log.info("Job cancelled: {}", inputData.getJobName());
            return createErrorResult("CANCELLED", "Job was cancelled by user");
        } catch (Exception e) {
            log.error("Execution failed for job: {}", inputData.getJobName(), e);
            return createErrorResult("FAILED", "Internal Engine Error: " + e.getMessage());
        }
    }

    private OutputData createErrorResult(String status, String reason) {
        OutputData error = new OutputData();
        error.setStatus(status);
        error.setMainErrorCode(status);
        error.setMainErrorReason(reason);
        return error;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Job Engine...");
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
