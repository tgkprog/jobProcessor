package com.example.smallapp;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Service
public class JobService {

    public static class JobHistory {
        public LocalDateTime scheduledTime;
        public String status;
        public String error;
        public LocalDateTime actualStartTime;
        public LocalDateTime actualEndTime;

        public JobHistory(LocalDateTime scheduledTime) {
            this.scheduledTime = scheduledTime;
            this.status = "Scheduled";
        }
    }

    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;
    private String status = "Not Set";
    private LocalDateTime scheduledTime;
    private int sleepSeconds = 20;
    private int randomSleepSeconds = 0;
    private final List<JobHistory> history = new ArrayList<>();
    private JobHistory currentHistoryItem;

    public JobService(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public synchronized void setSchedule(LocalDateTime time, int sleep, int randomSleep) {
        cancelSchedule();
        this.scheduledTime = time;
        this.sleepSeconds = sleep;
        this.randomSleepSeconds = randomSleep;
        this.status = "Scheduled at " + time + " (" + ZoneId.systemDefault() + ")";
        
        this.currentHistoryItem = new JobHistory(time);
        history.add(0, currentHistoryItem);
        if (history.size() > 5) {
            history.remove(history.size() - 1);
        }

        Instant instant = time.atZone(ZoneId.systemDefault()).toInstant();
        this.scheduledTask = taskScheduler.schedule(this::executeJob, instant);
    }

    public synchronized void cancelSchedule() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
            if (currentHistoryItem != null && "Scheduled".equals(currentHistoryItem.status)) {
                currentHistoryItem.status = "Cancelled";
            }
        }
        this.scheduledTime = null;
        this.status = "Idle";
        this.currentHistoryItem = null;
    }

    private void executeJob() {
        JobHistory item;
        synchronized (this) {
            this.status = "Working";
            item = this.currentHistoryItem;
            if (item != null) item.actualStartTime = LocalDateTime.now();
        }
        
        System.out.println("JOB STARTED at " + LocalDateTime.now());
        try {
            // Base sleep
            System.out.println("Sleeping for " + sleepSeconds + " seconds...");
            Thread.sleep(sleepSeconds * 1000L);

            // Random sleep
            if (randomSleepSeconds > 0) {
                long maxRandomMs = randomSleepSeconds * 1000L;
                long randMs = 1 + (long)(Math.random() * maxRandomMs);
                System.out.println("Random sleep for " + randMs + " ms...");
                Thread.sleep(randMs);
            }
            if (item != null) item.status = "Ran";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Job interrupted");
            if (item != null) {
                item.status = "Interrupted";
                item.error = e.getMessage();
            }
        } catch (Exception e) {
            System.err.println("Job failed: " + e.getMessage());
            if (item != null) {
                item.status = "Error";
                item.error = e.getMessage();
            }
        } finally {
            System.out.println("JOB ENDED at " + LocalDateTime.now());
            synchronized (this) {
                this.status = "Idle";
                this.scheduledTime = null;
                if (item != null) item.actualEndTime = LocalDateTime.now();
                this.currentHistoryItem = null;
            }
        }
    }

    public synchronized String getStatus() {
        return status;
    }

    public synchronized LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public synchronized List<JobHistory> getHistory() {
        return new ArrayList<>(history);
    }

    public String getTimeZone() {
        return ZoneId.systemDefault().toString();
    }
}
