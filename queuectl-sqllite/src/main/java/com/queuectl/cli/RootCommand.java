package com.queuectl.cli;

import picocli.CommandLine.Command;

@Command(name = "queuectl", mixinStandardHelpOptions = true, version = "queuectl 1.0",
         description = "CLI for queuectl job queue")
public class RootCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("queuectl - run 'queuectl --help' for usage");
    }
}
