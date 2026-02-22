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
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final Scheduler quartzScheduler;
    private final JobExecutionService jobExecutionService;

    @GetMapping("/status")
    public List<JobRecord> getStatus() {
        return jobRepository.findAll();
    }

    /**
     * Schedule a job to run at the earliest specified time.
     *
     * @param jobName              Name of the job
     * @param processorClassName   Fully qualified processor class name
     * @param comment              Optional comment
     * @param delayDays            Days from now (default 0)
     * @param delayHours           Hours from now (default 0)
     * @param delayMinutes         Minutes from now (default 0)
     */
    @PostMapping("/schedule")
    public JobRecord schedule(
            @RequestParam String jobName,
            @RequestParam String processorClassName,
            @RequestParam(required = false) String comment,
            @RequestParam(defaultValue = "0") int delayDays,
            @RequestParam(defaultValue = "0") int delayHours,
            @RequestParam(defaultValue = "0") int delayMinutes) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime runAt = now
                .plusDays(delayDays)
                .plusHours(delayHours)
                .plusMinutes(delayMinutes);

        // Save job to DB
        JobRecord job = new JobRecord();
        job.setJobName(jobName);
        job.setProcessorClassName(processorClassName);
        job.setComment(comment);
        job.setJobSubmittedDateTime(now);
        job.setScheduledRunTime(runAt);
        job.setStatus("SCHEDULED");
        job = jobRepository.save(job);

        // Schedule via Quartz
        try {
            scheduleQuartzJob(job.getId(), runAt);
            log.info("Job {} scheduled to run at {}", job.getId(), runAt);
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
