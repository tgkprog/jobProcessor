package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "JobProcessor")
@Data
public class ProcessorDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "processor_id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "class_name", unique = true, nullable = false)
    private String className;

    @Column(name = "jar_path", nullable = false)
    private String jarPath;

    @Column(name = "created_ts", updatable = false)
    private LocalDateTime createdTs;

    @Column(name = "updated_ts")
    private LocalDateTime updatedTs;

    @Column(length = 1)
    private String active = "Y";

    @PrePersist
    protected void onCreate() {
        createdTs = LocalDateTime.now();
        updatedTs = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTs = LocalDateTime.now();
    }
}
