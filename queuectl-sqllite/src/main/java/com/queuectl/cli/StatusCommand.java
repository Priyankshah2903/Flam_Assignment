package com.queuectl.cli;

import com.queuectl.core.SqliteJobRepository;
import com.queuectl.core.Job;
import com.queuectl.core.JobState;
import com.queuectl.core.WorkerManager;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "status", description = "Show counts per state and active workers")
public class StatusCommand implements Runnable {
    @Override
    public void run() {
        try {
            SqliteJobRepository repo = SqliteJobRepository.instance();
            List<Job> all = repo.listAll();
            Map<String, Long> counts = all.stream()
                    .collect(Collectors.groupingBy(j -> j.getState().name(), Collectors.counting()));
            System.out.println("Job counts by state:");
            counts.forEach((k, v) -> System.out.printf("  %s: %d%n", k, v));
            int workers = (WorkerManager.getExisting() == null) ? 0 : WorkerManager.getExisting().activeWorkerCount();
            System.out.println("Active workers in process: " + workers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
