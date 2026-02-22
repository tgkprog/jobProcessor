package com.sel2in.jobProc.service;

import com.sel2in.jobProc.processor.IJobProcessor;
import com.sel2in.jobProc.processor.InputData;
import com.sel2in.jobProc.processor.JobEstimate;
import com.sel2in.jobProc.processor.OutputData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;

@Service
@Slf4j
public class JobEngineService {

    private ExecutorService threadPool;
    
    @Autowired
    private DynamicClassLoaderService classLoaderService;

    @PostConstruct
    public void init() {
        // Default size, should be loaded from AppParams later
        threadPool = Executors.newFixedThreadPool(5);
    }

    public Future<OutputData> submitJob(InputData inputData, String jarPath) throws Exception {
        IJobProcessor processor = classLoaderService.loadProcessor(jarPath, inputData.getProcessorClassName());
        
        JobEstimate estimate = processor.acceptJob(inputData);
        long timeout = (long) (estimate.getMaxTimeToProcessMillis() * 1.5);

        return threadPool.submit(() -> {
            try {
                // Simplified execution. In real world, use a customized thread to handle interrupts better
                return CompletableFuture.supplyAsync(() -> processor.processJob(inputData))
                        .get(timeout, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                OutputData error = new OutputData();
                error.setStatus("TIMED_OUT");
                error.setMainErrorReason("Execution exceeded 150% of estimate: " + timeout + "ms");
                return error;
            } catch (Exception e) {
                OutputData error = new OutputData();
                error.setStatus("FAILED");
                error.setMainErrorReason(e.getMessage());
                return error;
            }
        });
    }
    
    public void updateThreadPoolSize(int newSize) {
        // More complex logic needed to resize existing pool gracefully
        log.info("Request to update thread pool size to {}", newSize);
    }
}
