package com.queuectl.cli;

import com.queuectl.core.SqliteJobRepository;
import com.queuectl.core.Job;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = "dlq", description = "Dead Letter Queue commands", subcommands = {DlqCommand.ListCmd.class, DlqCommand.Retry.class})
public class DlqCommand implements Runnable {
    @Override public void run() { System.out.println("dlq list | dlq retry <jobId>"); }

    @Command(name = "list", description = "List DLQ entries")
    public static class ListCmd implements Runnable {
        @Override
        public void run() {
            try {
                SqliteJobRepository repo = SqliteJobRepository.instance();
                List<Job> dead = repo.listByState(com.queuectl.core.JobState.DEAD);
                if (dead.isEmpty()) System.out.println("DLQ empty");
                else {
                    System.out.println("DLQ:");
                    dead.forEach(j -> System.out.printf("  id=%s cmd=\"%s\" attempts=%d failedAt=%s%n",
                            j.getId(), j.getCommand(), j.getAttempts(), j.getUpdatedAt()));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Command(name = "retry", description = "Retry a DLQ job by id")
    public static class Retry implements Runnable {
        @Parameters(index = "0", description = "Job id to retry")
        private String jobId;

        @Override
        public void run() {
            try {
                SqliteJobRepository repo = SqliteJobRepository.instance();
                boolean ok = repo.retryFromDlq(jobId);
                System.out.println(ok ? "Retried job " + jobId : "Job not found in DLQ: " + jobId);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
