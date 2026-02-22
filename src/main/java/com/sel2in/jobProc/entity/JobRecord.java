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
    private Long id;

    private String jobName;
    private String processorClassName;
    private String comment;
    private LocalDateTime jobSubmittedDateTime;
    private String status = "PENDING";
    private LocalDateTime jobStartDateTime;
    private LocalDateTime jobEndDateTime;
}
