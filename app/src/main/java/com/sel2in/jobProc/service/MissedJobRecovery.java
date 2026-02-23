package com.sel2in.jobProc.service;

import com.sel2in.jobProc.entity.JobRecord;
import com.sel2in.jobProc.repo.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * On server startup, finds any SCHEDULED jobs whose scheduled time has passed
 * (e.g., server was down) and triggers them to run immediately.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissedJobRecovery {

    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverMissedJobs() {
        List<JobRecord> scheduledJobs = jobRepository.findByStatus("SCHEDULED");
        LocalDateTime now = LocalDateTime.now();
        int recovered = 0;

        for (JobRecord job : scheduledJobs) {
            if (job.getScheduledRunTime() != null && job.getScheduledRunTime().isBefore(now)) {
                log.info("Recovering missed job {} '{}' (was scheduled for {})",
                        job.getId(), job.getJobName(), job.getScheduledRunTime());
                try {
                    jobExecutionService.runJob(job.getId());
                    recovered++;
                } catch (Exception e) {
                    log.error("Failed to recover missed job {}: {}", job.getId(), e.getMessage());
                }
            }
        }

        if (recovered > 0) {
            log.info("Recovered {} missed job(s) on startup", recovered);
        } else {
            log.info("No missed jobs to recover on startup");
        }
    }
}
