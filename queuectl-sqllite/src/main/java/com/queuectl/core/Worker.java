package com.queuectl.core;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Worker implements Runnable {
    private final JobRepository repo;
    private final String workerId;
    private final BackoffStrategy backoff;
    private final JobExecutor executor;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public Worker(JobRepository repo) {
        this.repo = repo;
        this.workerId = "worker-" + UUID.randomUUID();
        this.backoff = new BackoffStrategy(repo);
        this.executor = new JobExecutor();
    }

    public void stop() { running.set(false); }

    @Override
    public void run() {
        try {
            while (running.get()) {
                var opt = repo.claimNextDueJob(workerId);
                if (opt.isEmpty()) {
                    Thread.sleep(500);
                    continue;
                }
                Job job = opt.get();
                try {
                    int exit = executor.runCommand(job.getCommand(), 0);
                    if (exit == 0) repo.markCompleted(job.getId());
                    else handleFailure(job, "exit=" + exit);
                } catch (Exception e) {
                    handleFailure(job, e.toString());
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleFailure(Job job, String error) {
        try {
            int attempts = job.getAttempts();
            int max = job.getMaxRetries();
            if (attempts >= max) repo.moveToDlq(job, error);
            else {
                long delay = backoff.computeDelaySeconds(attempts);
                repo.scheduleRetry(job, delay, error);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
