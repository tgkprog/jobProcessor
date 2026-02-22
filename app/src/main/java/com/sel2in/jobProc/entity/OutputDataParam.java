package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "OutputDataParam")
@Data
public class OutputDataParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id")
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "param_name")
    private String paramName;

    @Column(name = "param_type")
    private String paramType;

    @Column(name = "string_value")
    private String stringValue;

    @Column(name = "number_value")
    private Double numberValue;

    @Column(name = "date_value")
    private LocalDateTime dateValue;

    @Column(name = "object_json", columnDefinition = "CLOB")
    private String objectJson;
}
