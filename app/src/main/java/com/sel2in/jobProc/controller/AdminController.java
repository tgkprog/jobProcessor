package com.sel2in.jobProc.controller;

import com.sel2in.jobProc.entity.AppParam;
import com.sel2in.jobProc.entity.JobRecord;
import com.sel2in.jobProc.repo.AppParamRepository;
import com.sel2in.jobProc.repo.JobRepository;
import com.sel2in.jobProc.service.JobEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin API for AppParams, thread pool, and job management.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AppParamRepository appParamRepository;
    private final JobEngine jobEngine;
    private final JobRepository jobRepository;

    // ===== AppParams =====

    @GetMapping("/params")
    public List<AppParam> listParams() {
        return appParamRepository.findAll();
    }

    @PostMapping("/params")
    public AppParam setParam(@RequestParam String name, @RequestParam String value,
                             @RequestParam(required = false) String description) {
        AppParam param = appParamRepository.findById(name).orElse(new AppParam());
        param.setName(name);
        param.setValue(value);
        if (description != null) {
            param.setDescription(description);
        }
        param = appParamRepository.save(param);
        log.info("AppParam set: {} = {}", name, value);

        // If thread pool size changed, apply immediately
        if ("numberOfThreads".equals(name)) {
            try {
                jobEngine.resizePool(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid numberOfThreads value: {}", value);
            }
        }

        return param;
    }

    // ===== Thread Pool =====

    @GetMapping("/engine/status")
    public Map<String, Object> engineStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("poolSize", jobEngine.getPoolSize());
        status.put("activeThreads", jobEngine.getActiveCount());
        status.put("activeJobIds", jobEngine.getActiveJobIds());
        return status;
    }

    // ===== Job Cancel =====

    @PostMapping("/job/cancel")
    public Map<String, Object> cancelJob(@RequestParam Long jobId) {
        boolean cancelled = jobEngine.cancelJob(jobId);
        if (cancelled) {
            // Update DB
            jobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus("CANCELLED");
                job.setJobEndDateTime(LocalDateTime.now());
                jobRepository.save(job);
            });
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId);
        result.put("cancelled", cancelled);
        return result;
    }
}
