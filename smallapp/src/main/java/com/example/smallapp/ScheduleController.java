package com.example.smallapp;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/small/api")
public class ScheduleController {

    private final JobService jobService;

    public ScheduleController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", jobService.getStatus());
        response.put("scheduledTime", jobService.getScheduledTime());
        response.put("history", jobService.getHistory());
        response.put("timezone", jobService.getTimeZone());
        response.put("currentTime", LocalDateTime.now());
        return response;
    }

    @PostMapping("/set")
    public Map<String, Object> setSchedule(@RequestBody Map<String, Object> request) {
        String timeStr = (String) request.get("time");
        int sleep = (int) request.getOrDefault("sleep", 20);
        int randomSleep = (int) request.getOrDefault("randomSleep", 0);
        
        LocalDateTime time = LocalDateTime.parse(timeStr);
        jobService.setSchedule(time, sleep, randomSleep);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Schedule set");
        response.put("serverTime", LocalDateTime.now());
        response.put("timezone", jobService.getTimeZone());
        return response;
    }

    @PostMapping("/cancel")
    public Map<String, Object> cancelSchedule() {
        jobService.cancelSchedule();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Schedule cleared");
        return response;
    }
}
