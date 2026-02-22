package com.sel2in.jobProc.service;

import com.sel2in.jobProc.entity.JobRecord;
import com.sel2in.jobProc.entity.ProcessorDefinition;
import com.sel2in.jobProc.processor.InputData;
import com.sel2in.jobProc.processor.OutputData;
import com.sel2in.jobProc.repo.JobRepository;
import com.sel2in.jobProc.repo.ProcessorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Loads a job from DB by ID and kicks off execution via JobEngine.
 * This is the method called by the Quartz trigger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobRepository jobRepository;
    private final ProcessorRepository processorRepository;
    private final JobEngine jobEngine;

    /**
     * Called by the scheduler when a job's scheduled time arrives.
     * Loads the job from DB, resolves the processor JAR path, and runs it.
     */
    public void runJob(Long jobId) {
        log.info("=== Scheduler triggered for job ID: {} ===", jobId);

        Optional<JobRecord> optJob = jobRepository.findById(jobId);
        if (optJob.isEmpty()) {
            log.error("Job ID {} not found in database, skipping.", jobId);
            return;
        }

        JobRecord job = optJob.get();

        if (!"SCHEDULED".equals(job.getStatus())) {
            log.warn("Job {} is in status '{}', expected SCHEDULED. Skipping.", jobId, job.getStatus());
            return;
        }

        // Resolve JAR path from ProcessorDefinition
        Optional<ProcessorDefinition> optProc = processorRepository.findByClassName(job.getProcessorClassName());
        String jarPath;
        if (optProc.isPresent()) {
            jarPath = optProc.get().getJarPath();
        } else {
            log.warn("No processor registered for class '{}', using default path.", job.getProcessorClassName());
            jarPath = "./processors/" + job.getProcessorClassName().substring(
                    job.getProcessorClassName().lastIndexOf('.') + 1) + ".jar";
        }

        // Mark as RUNNING
        job.setStatus("RUNNING");
        job.setJobStartDateTime(LocalDateTime.now());
        jobRepository.save(job);

        // Build InputData from DB record
        InputData inputData = new InputData();
        inputData.setInputDataId(job.getId());
        inputData.setJobName(job.getJobName());
        inputData.setProcessorClassName(job.getProcessorClassName());
        inputData.setComment(job.getComment());
        inputData.setNotes(job.getNotes());

        // Execute async and update DB when done
        jobEngine.executeAsync(inputData, jarPath).thenAccept(output -> {
            job.setJobEndDateTime(LocalDateTime.now());
            job.setStatus(output.getStatus() != null ? output.getStatus() : "SUCCESS");
            jobRepository.save(job);
            log.info("Job {} completed with status: {}", jobId, job.getStatus());
        }).exceptionally(ex -> {
            job.setJobEndDateTime(LocalDateTime.now());
            job.setStatus("FAILED");
            jobRepository.save(job);
            log.error("Job {} failed: {}", jobId, ex.getMessage());
            return null;
        });
    }
}
