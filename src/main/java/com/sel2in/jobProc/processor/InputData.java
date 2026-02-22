package com.sel2in.jobProc.processor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class InputData implements Serializable {
    private Long inputDataId;
    private String jobName;
    private String processorClassName;
    private String comment;
    private String notes;
    private Map<String, Object> parameters;
    private List<String> inputFiles; // Paths
    private java.util.Date jobSubmittedDateTime;
    private String jobSubmittedTimeZone;
}
