package com.sel2in.jobProc.processor;

/**
 * Interface definition for external Job Processors.
 * Implementations of this interface should be packaged in external JAR files
 * to be loaded dynamically by the Job Engine.
 */
public interface JobProcessor {

    /**
     * Reviews the input data before processing and provides an estimate 
     * of the resources or time required.
     */
    JobEstimate reviewJob(InputData inputData);

    /**
     * Executes the actual job processing logic.
     */
    OutputData processJob(InputData inputData);
}
