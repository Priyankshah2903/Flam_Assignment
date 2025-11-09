package com.queuectl.core;

import java.util.Map;
import java.util.Random;

public class BackoffStrategy {
    private final JobRepository repo;
    private final Random rnd = new Random();

    public BackoffStrategy(JobRepository repo) { this.repo = repo; }

    public long computeDelaySeconds(int attempts) {
        try {
            Map<String, String> cfg = repo.getConfig();
            double base = Double.parseDouble(cfg.getOrDefault("backoff_base", "2"));
            long maxBackoff = Long.parseLong(cfg.getOrDefault("max_backoff_seconds", "3600"));
            double jitter = Double.parseDouble(cfg.getOrDefault("jitter_percent", "0.1"));
            double raw = Math.pow(base, Math.max(1, attempts));
            double jitterFactor = 1.0 + (rnd.nextDouble() * 2 - 1) * jitter;
            long delay = Math.min(maxBackoff, (long) Math.max(1, Math.round(raw * jitterFactor)));
            return delay;
        } catch (Exception e) {
            double raw = Math.pow(2, Math.max(1, attempts));
            return Math.min(3600, (long) raw);
        }
    }
}
