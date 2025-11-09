package com.queuectl.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbUtils {
    private static final String DB_DIR = "data";
    private static final String DB_PATH = DB_DIR + "/queue.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    public static Connection getConnection() throws Exception {
        ensureDb();
        return DriverManager.getConnection(JDBC_URL);
    }

    private static void ensureDb() throws Exception {
        File d = new File(DB_DIR);
        if (!d.exists()) d.mkdirs();
        File f = new File(DB_PATH);
        boolean existed = f.exists();
        if (!existed) {
            try (Connection c = DriverManager.getConnection(JDBC_URL);
                 Statement s = c.createStatement()) {
                s.executeUpdate("PRAGMA foreign_keys = ON;");
                s.executeUpdate("CREATE TABLE jobs (" +
                        "id TEXT PRIMARY KEY, " +
                        "command TEXT NOT NULL, " +
                        "state TEXT NOT NULL, " +
                        "attempts INTEGER NOT NULL DEFAULT 0, " +
                        "max_retries INTEGER NOT NULL DEFAULT 3, " +
                        "created_at TEXT NOT NULL, " +
                        "updated_at TEXT NOT NULL, " +
                        "next_run_at TEXT, " +
                        "last_error TEXT, " +
                        "worker_id TEXT" +
                        ");");
                s.executeUpdate("CREATE TABLE config (key TEXT PRIMARY KEY, value TEXT);");
                // default config
                s.executeUpdate("INSERT INTO config(key, value) VALUES('backoff_base','2');");
                s.executeUpdate("INSERT INTO config(key, value) VALUES('max_backoff_seconds','3600');");
                s.executeUpdate("INSERT INTO config(key, value) VALUES('jitter_percent','0.1');");
            }
        }
    }
}
