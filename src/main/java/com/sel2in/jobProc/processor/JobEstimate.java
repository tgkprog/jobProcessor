package com.sel2in.jobProc.processor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobEstimate {
    private long maxTimeToProcessMillis;
}
