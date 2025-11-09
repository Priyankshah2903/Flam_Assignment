package com.queuectl.cli;

import com.queuectl.core.SqliteJobRepository;
import com.queuectl.core.Job;
import com.queuectl.core.JobState;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "list", description = "List jobs by state")
public class ListCommand implements Runnable {
    @Option(names = {"--state"}, description = "State filter (pending, processing, completed, failed, dead)", defaultValue = "pending")
    private String state;

    @Override
    public void run() {
        try {
            SqliteJobRepository repo = SqliteJobRepository.instance();
            List<Job> jobs = repo.listByState(JobState.valueOf(state.toUpperCase()));
            System.out.println("Jobs in state " + state + ":");
            for (Job j : jobs) {
                System.out.printf("  id=%s attempts=%d max=%d cmd=\"%s\" next_run=%s%n",
                        j.getId(), j.getAttempts(), j.getMaxRetries(), j.getCommand(),
                        j.getNextRunAt() == null ? "-" : j.getNextRunAt().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
