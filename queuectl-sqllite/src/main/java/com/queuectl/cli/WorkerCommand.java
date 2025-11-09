package com.queuectl.cli;

import com.queuectl.core.SqliteJobRepository;
import com.queuectl.core.WorkerManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "worker", description = "Manage workers", subcommands = {WorkerCommand.Start.class, WorkerCommand.Stop.class})
public class WorkerCommand implements Runnable {
    @Override
    public void run() { System.out.println("Use 'worker start' or 'worker stop'."); }

    @Command(name = "start", description = "Start workers in this process")
    public static class Start implements Runnable {
        @Option(names = {"--count"}, description = "Number of worker threads", defaultValue = "1")
        private int count;

        @Override
        public void run() {
            try {
                SqliteJobRepository repo = SqliteJobRepository.instance();
                WorkerManager manager = WorkerManager.getInstance(repo);
                manager.start(count);
                System.out.println("Workers started. Press Ctrl+C to stop.");
                manager.awaitTermination();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Command(name = "stop", description = "Stop workers (if running in same JVM)")
    public static class Stop implements Runnable {
        @Override
        public void run() {
            WorkerManager manager = WorkerManager.getExisting();
            if (manager != null) {
                manager.stop();
                System.out.println("Stop requested.");
            } else {
                System.out.println("No worker manager found in this process.");
            }
        }
    }
}
