package com.sel2in.jobProc.controller;

import com.sel2in.jobProc.entity.InputDataFile;
import com.sel2in.jobProc.entity.InputDataParam;
import com.sel2in.jobProc.entity.JobRecord;
import com.sel2in.jobProc.repo.InputDataFileRepository;
import com.sel2in.jobProc.repo.InputDataParamRepository;
import com.sel2in.jobProc.repo.JobRepository;
import com.sel2in.jobProc.service.JobExecutionService;
import com.sel2in.jobProc.service.ScheduledJobTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    /** Minimum delay before a job can run (seconds) */
    private static final long MIN_DELAY_SECONDS = 30;

    private final JobRepository jobRepository;
    private final InputDataFileRepository inputDataFileRepository;
    private final InputDataParamRepository inputDataParamRepository;
    private final Scheduler quartzScheduler;
    private final JobExecutionService jobExecutionService;
    private final com.sel2in.jobProc.service.JobEngine jobEngine;
    
    @org.springframework.beans.factory.annotation.Value("${jobproc.inputFileDirectory:./inputFiles}")
    private String inputFileDirectory;

    @GetMapping("/serverTime")
    @ResponseBody
    public String serverTime() {
        String dateTime = LocalDateTime.now().toString().replace("T", " ").substring(0, 19);
        String timeZone = java.time.ZoneId.systemDefault().getId();
        return dateTime + " " + timeZone;
    }

    @GetMapping("/status")
    public List<JobRecord> getStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        // Input validation
        if (page < 0) page = 0;
        if (size < 1) size = 50;
        if (size > 100) size = 100;
        
        if (page == 0 && size == 50) {
            // Legacy behavior for backward compatibility
            return jobRepository.findAll();
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<JobRecord> resultPage = jobRepository.findAll(pageable);
        return resultPage.getContent();
    }

    @GetMapping("/stats")
    public java.util.Map<String, Object> getStats() {
        List<JobRecord> jobs = jobRepository.findAll();
        java.util.Map<String, Long> statusCounts = jobs.stream()
                .collect(java.util.stream.Collectors.groupingBy(JobRecord::getStatus, java.util.stream.Collectors.counting()));

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", jobs.size());
        stats.put("statusCounts", statusCounts);
        
        // Last 10 finished jobs for history chart
        List<JobRecord> history = jobs.stream()
                .filter(j -> j.getJobEndDateTime() != null)
                .sorted((a, b) -> b.getJobEndDateTime().compareTo(a.getJobEndDateTime()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
        stats.put("history", history);

        return stats;
    }

    /**
     * Schedule a job to run at the earliest specified time.
     * Minimum delay is 30 seconds from now.
     *
     * @param jobName              Name of the job
     * @param processorClassName   Fully qualified processor class name
     * @param comment              Optional comment
     * @param delayDays            Days from now (default 0)
     * @param delayHours           Hours from now (default 0)
     * @param delayMinutes         Minutes from now (default 1)
     * @param files                Optional input files to upload
     */
    @PostMapping("/schedule")
    public JobRecord schedule(
            @RequestParam String jobName,
            @RequestParam String processorClassName,
            @RequestParam(required = false) String comment,
            @RequestParam(defaultValue = "0") int delayDays,
            @RequestParam(defaultValue = "0") int delayHours,
            @RequestParam(defaultValue = "1") int delayMinutes,
            @RequestParam(required = false) String inputData,
            @RequestParam(required = false) List<MultipartFile> files) throws IOException {

        // Input validation
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalArgumentException("jobName cannot be empty");
        }
        if (processorClassName == null || processorClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("processorClassName cannot be empty");
        }
        if (processorClassName.contains("/") || processorClassName.contains("\\") || processorClassName.contains("..")) {
            throw new IllegalArgumentException("processorClassName contains invalid characters (/, \\, ..)");
        }
        if (delayDays < 0) delayDays = 0;
        if (delayHours < 0) delayHours = 0;
        if (delayMinutes < 0) delayMinutes = 0;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requested = now
                .plusDays(delayDays)
                .plusHours(delayHours)
                .plusMinutes(delayMinutes);

        // Enforce minimum 30-second delay — this is a cron scheduler, not immediate execution
        LocalDateTime earliest = now.plusSeconds(MIN_DELAY_SECONDS);
        LocalDateTime runAt = requested.isBefore(earliest) ? earliest : requested;

        // Save job to DB
        JobRecord job = new JobRecord();
        job.setJobName(jobName);
        job.setProcessorClassName(processorClassName);
        job.setComment(comment);
        job.setJobSubmittedDateTime(now);
        job.setScheduledRunTime(runAt);
        job.setStatus("SCHEDULED");
        job = jobRepository.save(job);

        // Handle File Uploads
        if (files != null && !files.isEmpty()) {
            Path jobInputDir = Paths.get(inputFileDirectory, job.getId().toString());
            Files.createDirectories(jobInputDir);

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String fileName = file.getOriginalFilename();
                Path filePath = jobInputDir.resolve(fileName).toAbsolutePath();
                file.transferTo(filePath.toFile());

                InputDataFile idf = new InputDataFile();
                idf.setInputDataId(job.getId());
                idf.setFileName(fileName);
                idf.setFilePath(filePath.toString());
                idf.setFileSize(file.getSize());
                inputDataFileRepository.save(idf);
                log.info("Saved input file: {} for job {}", fileName, job.getId());
            }
        }

        // Handle Input Parameters JSON
        if (inputData != null && !inputData.trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> params = mapper.readValue(inputData, Map.class);
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    InputDataParam param = new InputDataParam();
                    param.setInputDataId(job.getId());
                    param.setParamName(entry.getKey());
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        param.setParamType("NUMBER");
                        param.setNumberValue(((Number) value).doubleValue());
                    } else {
                        param.setParamType("STRING");
                        param.setStringValue(value.toString());
                    }
                    inputDataParamRepository.save(param);
                    log.info("Saved input param: {}={} for job {}", entry.getKey(), value, job.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to parse inputData JSON for job {}: {}", job.getId(), e.getMessage());
            }
        }

        long delaySec = ChronoUnit.SECONDS.between(now, runAt);
        log.info("Job {} '{}' will run in {}s at {}", job.getId(), jobName, delaySec, runAt);

        // Schedule via Quartz
        try {
            scheduleQuartzJob(job.getId(), runAt);
        } catch (SchedulerException e) {
            log.error("Failed to schedule Quartz trigger for job {}", job.getId(), e);
            job.setStatus("SCHEDULE_FAILED");
            job.setNotes("Quartz error: " + e.getMessage());
            jobRepository.save(job);
        }

        return job;
    }

    /**
     * Manual trigger: reschedule a job to run in 3 seconds.
     * Only allowed if the job is SCHEDULED and its current scheduled time is 
     * more than 5 seconds away (handled primarily by UI, but enforced here).
     */
    @GetMapping("/run")
    public String runNow(@RequestParam Long jobId) {
        JobRecord job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return "Job not found";

        if (!"SCHEDULED".equals(job.getStatus()) && !"FAILED".equals(job.getStatus())) {
            return "Job is in status " + job.getStatus() + ", cannot trigger run now";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime runAt = now.plusSeconds(3);

        // Update DB
        job.setStatus("SCHEDULED");
        job.setScheduledRunTime(runAt);
        jobRepository.save(job);

        // Reschedule via Quartz
        try {
            // Remove old trigger/job if exists
            quartzScheduler.unscheduleJob(new TriggerKey("trigger-" + jobId, "jobproc"));
            quartzScheduler.deleteJob(new JobKey("job-" + jobId, "jobproc"));

            // Schedule fresh
            scheduleQuartzJob(jobId, runAt);
        } catch (SchedulerException e) {
            log.error("Failed to reschedule Quartz trigger for job {}", jobId, e);
            return "Error rescheduling job: " + e.getMessage();
        }

        return "Job " + jobId + " rescheduled to run in 3 seconds";
    }

    private void scheduleQuartzJob(Long jobId, LocalDateTime runAt) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(ScheduledJobTrigger.class)
                .withIdentity("job-" + jobId, "jobproc")
                .usingJobData("jobId", jobId)
                .build();

        Date triggerTime = Date.from(runAt.atZone(ZoneId.systemDefault()).toInstant());

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + jobId, "jobproc")
                .startAt(triggerTime)
                .build();

        quartzScheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * Cancel a running job.
     * @param jobId The job ID to cancel
     */
    @PostMapping("/cancel")
    public java.util.Map<String, Object> cancelJob(@RequestParam Long jobId) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        JobRecord job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            response.put("success", false);
            response.put("message", "Job not found");
            return response;
        }

        if (!"RUNNING".equals(job.getStatus())) {
            response.put("success", false);
            response.put("message", "Job is not running (status: " + job.getStatus() + ")");
            return response;
        }

        boolean cancelled = jobEngine.cancelJob(jobId);
        
        if (cancelled) {
            job.setStatus("CANCELLED");
            job.setJobEndDateTime(LocalDateTime.now());
            job.setErrorReason("Job cancelled by user");
            jobRepository.save(job);
            
            response.put("success", true);
            response.put("message", "Job " + jobId + " cancelled successfully");
        } else {
            response.put("success", false);
            response.put("message", "Failed to cancel job " + jobId);
        }
        
        return response;
    }

    /**
     * Returns up to 3 input and 3 output file names for a given job.
     * Used by the jobs UI to show inline file links.
     */
    @GetMapping("/files/{jobId}")
    public Map<String, Object> getJobFiles(@PathVariable Long jobId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId);

        // Input files from DB
        List<com.sel2in.jobProc.entity.InputDataFile> dbFiles =
                inputDataFileRepository.findByInputDataId(jobId);
        List<Map<String, String>> inputs = new ArrayList<>();
        if (dbFiles != null) {
            for (int i = 0; i < Math.min(dbFiles.size(), 3); i++) {
                Map<String, String> f = new LinkedHashMap<>();
                f.put("name", dbFiles.get(i).getFileName());
                f.put("url", "/dwn/input/" + jobId + "/" + dbFiles.get(i).getFileName());
                inputs.add(f);
            }
        }
        result.put("inputFiles", inputs);

        // Output files from disk
        List<Map<String, String>> outputs = new ArrayList<>();
        java.nio.file.Path outDir = java.nio.file.Paths.get("./outputFiles", String.valueOf(jobId));
        if (java.nio.file.Files.exists(outDir) && java.nio.file.Files.isDirectory(outDir)) {
            try (var stream = java.nio.file.Files.list(outDir)) {
                List<java.nio.file.Path> files = stream
                        .filter(java.nio.file.Files::isRegularFile)
                        .sorted()
                        .limit(3)
                        .collect(Collectors.toList());
                for (java.nio.file.Path p : files) {
                    Map<String, String> f = new LinkedHashMap<>();
                    f.put("name", p.getFileName().toString());
                    f.put("url", "/dwn/" + jobId + "/" + p.getFileName().toString());
                    outputs.add(f);
                }
            } catch (IOException e) {
                log.warn("Could not list output files for job {}", jobId, e);
            }
        }
        result.put("outputFiles", outputs);

        return result;
    }
}
