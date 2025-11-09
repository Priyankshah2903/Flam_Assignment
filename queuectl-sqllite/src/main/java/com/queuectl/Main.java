package com.queuectl;

import com.queuectl.cli.*;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        CommandLine root = new CommandLine(new RootCommand());
        root.addSubcommand("enqueue", new EnqueueCommand());
        root.addSubcommand("worker", new WorkerCommand());
        root.addSubcommand("status", new StatusCommand());
        root.addSubcommand("list", new ListCommand());
        root.addSubcommand("dlq", new DlqCommand());
        root.addSubcommand("config", new ConfigCommand());
        System.exit(root.execute(args));
    }
}
