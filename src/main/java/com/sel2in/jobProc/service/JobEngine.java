package com.sel2in.jobProc.service;

import com.sel2in.jobProc.processor.JobProcessor;
import com.sel2in.jobProc.processor.InputData;
import com.sel2in.jobProc.processor.JobEstimate;
import com.sel2in.jobProc.processor.OutputData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobEngine {

    private final ProcessorLoader processorLoader;
    private ExecutorService executorService;

    @PostConstruct
    public void startup() {
        // Initial thread pool size. 
        // TODO: In production, load this from AppParams / configuration properties
        int poolSize = 5;
        log.info("Starting Job Engine with thread pool size: {}", poolSize);
        executorService = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Submits a job for asynchronous execution.
     * 
     * @param inputData The data defining the job.
     * @param jarPath The location of the JAR containing the processor.
     * @return A CompletableFuture that will contain the OutputData result.
     */
    public CompletableFuture<OutputData> executeAsync(InputData inputData, String jarPath) {
        return CompletableFuture.supplyAsync(() -> execute(inputData, jarPath), executorService);
    }

    private OutputData execute(InputData inputData, String jarPath) {
        String className = inputData.getProcessorClassName();
        log.info("Executing job: {} using processor: {}", inputData.getJobName(), className);

        try {
            JobProcessor processor = processorLoader.load(jarPath, className);
            
            // 1. Review the job to get estimates
            JobEstimate estimate = processor.reviewJob(inputData);
            long timeoutMillis = (long) (estimate.getMaxTimeToProcessMillis() * 1.5);
            log.debug("Job timeout calculated as: {}ms", timeoutMillis);

            // 2. Process with timeout
            // We use a nested future to enforce the per-job timeout
            return CompletableFuture.supplyAsync(() -> processor.processJob(inputData))
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            log.error("Job timed out: {}", inputData.getJobName());
            return createErrorResult("TIMED_OUT", "Execution exceeded allowed time: " + className);
        } catch (Exception e) {
            log.error("Execution failed for job: {}", inputData.getJobName(), e);
            return createErrorResult("FAILED", "Internal Engine Error: " + e.getMessage());
        }
    }

    private OutputData createErrorResult(String status, String reason) {
        OutputData error = new OutputData();
        error.setStatus(status);
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
