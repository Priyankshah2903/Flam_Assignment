package com.queuectl.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {
    private String id;
    private String command;
    private JobState state;
    private Integer attempts;
    private Integer maxRetries;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant nextRunAt;
    private String lastError;
    private String workerId;

    public Job() {}

    // getters / setters (omitted here for brevity — include all typical ones)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public JobState getState() { return state; }
    public void setState(JobState state) { this.state = state; }
    public Integer getAttempts() { return attempts == null ? 0 : attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
}
