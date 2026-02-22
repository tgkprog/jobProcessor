package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "OutputData")
@Data
public class OutputDataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long id;

    @Column(name = "input_data_id")
    private Long inputDataId;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "processor_class_name")
    private String processorClassName;

    @Column(name = "output_command")
    private String outputCommand;

    @Column(name = "output_note")
    private String outputNote;

    @Column(name = "job_start_datetime")
    private LocalDateTime jobStartDateTime;

    @Column(name = "job_end_datetime")
    private LocalDateTime jobEndDateTime;

    @Column(name = "job_start_timezone")
    private String jobStartTimezone;

    @Column(name = "job_end_timezone")
    private String jobEndTimezone;

    @Column(name = "main_error_reason")
    private String mainErrorReason;

    private String status;

    @Column(name = "created_ts", updatable = false)
    private LocalDateTime createdTs;

    @PrePersist
    protected void onCreate() {
        createdTs = LocalDateTime.now();
    }
}
