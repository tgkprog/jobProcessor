package com.sel2in.jobProc.samples;

import com.sel2in.jobProc.processor.InputData;
import com.sel2in.jobProc.processor.JobEstimate;
import com.sel2in.jobProc.processor.JobProcessor;
import com.sel2in.jobProc.processor.OutputData;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple example JobProcessor implementation.
 * It waits for a few seconds and returns a success status.
 */
public class SimpleProcessor implements JobProcessor {

    @Override
    public JobEstimate reviewJob(InputData inputData) {
        // Estimate 10 seconds max
        return new JobEstimate(10000);
    }

    @Override
    public OutputData processJob(InputData inputData) {
        System.out.println("SimpleProcessor: Starting job: " + inputData.getJobName());
        
        try {
            // Simulate work
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        OutputData output = new OutputData();
        output.setInputDataId(inputData.getInputDataId());
        output.setStatus("SUCCESS");
        output.setOutputNote("Job processed successfully by SimpleProcessor.");
        
        Map<String, Object> params = new HashMap<>();
        params.put("processedAt", new java.util.Date().toString());
        output.setOutputParameters(params);

        System.out.println("SimpleProcessor: Completed job: " + inputData.getJobName());
        return output;
    }
}
