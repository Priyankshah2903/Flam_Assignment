package com.queuectl.cli;

import com.queuectl.core.SqliteJobRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;

@Command(name = "config", description = "View or set configuration")
public class ConfigCommand implements Runnable {
    @Option(names = {"--set"}, arity = "2", description = "Set config: key value")
    String[] set;

    @Override
    public void run() {
        try {
            SqliteJobRepository repo = SqliteJobRepository.instance();
            if (set != null) {
                repo.setConfig(set[0], set[1]);
                System.out.println("Config set: " + set[0] + "=" + set[1]);
            } else {
                Map<String, String> cfg = repo.getConfig();
                System.out.println("Config:");
                cfg.forEach((k,v) -> System.out.println("  " + k + "=" + v));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
