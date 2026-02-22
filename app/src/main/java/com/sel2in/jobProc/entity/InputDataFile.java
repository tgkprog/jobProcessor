package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "InputDataFile")
@Data
public class InputDataFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Column(name = "input_data_id")
    private Long inputDataId;

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
