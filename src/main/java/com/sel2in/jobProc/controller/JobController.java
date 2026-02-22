package com.sel2in.jobProc.controller;

import com.sel2in.jobProc.entity.JobRecord;
import com.sel2in.jobProc.repo.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;

    @GetMapping("/status")
    public List<JobRecord> getStatus() {
        return jobRepository.findAll();
    }

    @PostMapping("/schedule")
    public JobRecord schedule(@RequestParam String jobName, 
                             @RequestParam String processorClassName,
                             @RequestParam(required = false) String comment) {
        JobRecord job = new JobRecord();
        job.setJobName(jobName);
        job.setProcessorClassName(processorClassName);
        job.setComment(comment);
        job.setJobSubmittedDateTime(LocalDateTime.now());
        job.setStatus("SCHEDULED");
        return jobRepository.save(job);
    }
}
