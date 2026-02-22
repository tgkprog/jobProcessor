package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "OutputDataFile")
@Data
public class OutputDataFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_ts", updatable = false)
    private LocalDateTime createdTs;

    @PrePersist
    protected void onCreate() {
        createdTs = LocalDateTime.now();
    }
}
