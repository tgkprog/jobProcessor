package com.sel2in.jobProc.processor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class OutputData implements Serializable {
    private Long jobId;
    private Long inputDataId;
    private String outputCommand;
    private String outputNote;
    private Map<String, Object> outputParameters;
    private List<String> outputFiles;
    private java.util.Date jobStartDateTime;
    private java.util.Date jobEndDateTime;
    private String status; // SUCCESS, FAILED, TIMED_OUT
    private String mainErrorReason;
}
