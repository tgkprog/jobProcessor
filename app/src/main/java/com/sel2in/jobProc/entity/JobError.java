package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "JobError")
@Data
public class JobError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_string")
    private String reasonString;

    @Column(name = "created_ts", updatable = false)
    private LocalDateTime createdTs;

    @PrePersist
    protected void onCreate() {
        createdTs = LocalDateTime.now();
    }
}
