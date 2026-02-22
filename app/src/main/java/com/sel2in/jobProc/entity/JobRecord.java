package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "InputData")
@Data
public class JobRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "input_data_id")
    private Long id;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "processor_class_name")
    private String processorClassName;

    private String comment;
    private String notes;

    @Column(name = "job_submitted_datetime")
    private LocalDateTime jobSubmittedDateTime;

    @Column(name = "job_submitted_timezone")
    private String jobSubmittedTimeZone;

    /** Earliest time this job is allowed to run */
    @Column(name = "scheduled_run_time")
    private LocalDateTime scheduledRunTime;

    private String status = "PENDING";

    @Column(name = "job_start_datetime")
    private LocalDateTime jobStartDateTime;

    @Column(name = "job_end_datetime")
    private LocalDateTime jobEndDateTime;

    @Column(name = "created_ts", updatable = false)
    private LocalDateTime createdTs;

    @PrePersist
    protected void onCreate() {
        createdTs = LocalDateTime.now();
    }
}
