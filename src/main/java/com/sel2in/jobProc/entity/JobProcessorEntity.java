package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "JobProcessor")
@Data
public class JobProcessorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long processor_id;

    @Column(name = "class_name", unique = true, nullable = false)
    private String className;

    @Column(name = "jar_path", nullable = false)
    private String jarPath;

    @Column(name = "created_ts")
    private LocalDateTime createdTs;

    @Column(name = "updated_ts")
    private LocalDateTime updatedTs;

    private String active = "Y";
}
