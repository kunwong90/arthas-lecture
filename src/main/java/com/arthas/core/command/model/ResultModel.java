package com.arthas.core.command.model;

/**
 * Command execute result
 */
public abstract class ResultModel {

    private int jobId;

    /**
     * Command type (name)
     *
     * @return
     */
    public abstract String getType();


    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
