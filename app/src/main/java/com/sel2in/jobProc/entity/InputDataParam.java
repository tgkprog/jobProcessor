package com.sel2in.jobProc.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "InputDataParam")
@Data
public class InputDataParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id")
    private Long id;

    @Column(name = "input_data_id")
    private Long inputDataId;

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
