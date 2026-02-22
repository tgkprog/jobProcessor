package com.sel2in.jobProc.controller;

import com.sel2in.jobProc.entity.JobRecord;
import com.sel2in.jobProc.repo.JobRepository;
import com.sel2in.jobProc.service.JobExecutionService;
import com.sel2in.jobProc.service.ScheduledJobTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    /** Minimum delay before a job can run (seconds) */
    private static final long MIN_DELAY_SECONDS = 30;

    private final JobRepository jobRepository;
    private final Scheduler quartzScheduler;
    private final JobExecutionService jobExecutionService;

    @GetMapping("/status")
    public List<JobRecord> getStatus() {
        return jobRepository.findAll();
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
     */
    @PostMapping("/schedule")
    public JobRecord schedule(
            @RequestParam String jobName,
            @RequestParam String processorClassName,
            @RequestParam(required = false) String comment,
            @RequestParam(defaultValue = "0") int delayDays,
            @RequestParam(defaultValue = "0") int delayHours,
            @RequestParam(defaultValue = "1") int delayMinutes) {

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
     * Manual trigger: run a job immediately by ID.
     * Useful for re-running failed jobs or testing.
     */
    @GetMapping("/run")
    public String runNow(@RequestParam Long jobId) {
        jobExecutionService.runJob(jobId);
        return "Job " + jobId + " triggered";
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
}
