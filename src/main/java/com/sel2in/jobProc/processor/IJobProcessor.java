package com.sel2in.jobProc.processor;

/**
 * Interface for external Job Processors
 */
public interface IJobProcessor {

    /**
     * Analyze input and return estimate
     */
    JobEstimate acceptJob(InputData inputData);

    /**
     * Perform the actual processing
     */
    OutputData processJob(InputData inputData);
}
