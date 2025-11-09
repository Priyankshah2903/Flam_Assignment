package com.queuectl.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Cross-platform JobExecutor.
 * Uses cmd.exe /c on Windows, /bin/sh -c on Unix-like OSes.
 */
public class JobExecutor {
    private final boolean isWindows;

    public JobExecutor() {
        String os = System.getProperty("os.name");
        isWindows = os != null && os.toLowerCase().contains("win");
    }

    /**
     * Run the given command string in platform shell.
     * If timeoutSeconds <= 0, wait indefinitely for completion.
     * Returns the process exit code (-2 = killed for timeout).
     */
    public int runCommand(String command, long timeoutSeconds) throws Exception {
        ProcessBuilder pb;
        if (isWindows) {
            pb = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            pb = new ProcessBuilder("/bin/sh", "-c", command);
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // drain output so process doesn't block
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        Thread logThread = new Thread(() -> {
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[cmd] " + line);
                }
            } catch (Exception ignored) { }
        }, "job-output-reader");
        logThread.setDaemon(true);
        logThread.start();

        if (timeoutSeconds > 0) {
            boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return -2;
            }
        }
        int exit = p.waitFor();
        try { logThread.join(200); } catch (InterruptedException ignored) {}
        return exit;
    }
}