package com.queuectl.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class WorkerManager {
    private static WorkerManager INSTANCE;
    private final JobRepository repo;
    private ExecutorService pool;
    private final List<Worker> workers = new ArrayList<>();

    private WorkerManager(JobRepository repo) { this.repo = repo; }

    public static synchronized WorkerManager getInstance(JobRepository repo) {
        if (INSTANCE == null) INSTANCE = new WorkerManager(repo);
        return INSTANCE;
    }

    public static WorkerManager getExisting() { return INSTANCE; }

    public void start(int count) {
        if (pool != null) throw new IllegalStateException("already started");
        pool = Executors.newFixedThreadPool(count);
        for (int i = 0; i < count; i++) {
            Worker w = new Worker(repo);
            workers.add(w);
            pool.submit(w);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop() {
        if (pool == null) return;
        for (Worker w : workers) w.stop();
        pool.shutdown();
        try { pool.awaitTermination(30, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally { pool = null; workers.clear(); }
    }

    public void awaitTermination() throws InterruptedException {
        if (pool != null) {
            while (!pool.isTerminated()) Thread.sleep(1000);
        }
    }

    public int activeWorkerCount() { return workers.size(); }
}
