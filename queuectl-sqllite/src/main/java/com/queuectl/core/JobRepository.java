package com.queuectl.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JobRepository {
    void addJob(Job j) throws Exception;
    List<Job> listAll() throws Exception;
    List<Job> listByState(JobState state) throws Exception;
    Optional<Job> claimNextDueJob(String workerId) throws Exception;
    void markCompleted(String jobId) throws Exception;
    void scheduleRetry(Job job, long delaySeconds, String error) throws Exception;
    void moveToDlq(Job job, String error) throws Exception;
    boolean retryFromDlq(String jobId) throws Exception;
    Map<String, String> getConfig() throws Exception;
    void setConfig(String key, String value) throws Exception;
}
