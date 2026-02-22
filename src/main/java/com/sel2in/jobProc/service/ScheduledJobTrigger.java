package com.sel2in.jobProc.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quartz Job that fires at the scheduled time.
 * Reads the jobId from the JobDataMap and delegates to JobExecutionService.
 */
@Slf4j
@Component
public class ScheduledJobTrigger implements Job {

    @Autowired
    private JobExecutionService jobExecutionService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long jobId = context.getJobDetail().getJobDataMap().getLong("jobId");
        log.info("Quartz trigger fired for jobId: {}", jobId);
        jobExecutionService.runJob(jobId);
    }
}
