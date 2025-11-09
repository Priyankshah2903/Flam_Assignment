package com.queuectl.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.queuectl.util.DbUtils;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class SqliteJobRepository implements JobRepository {
    private static SqliteJobRepository INSTANCE;
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private SqliteJobRepository() { /* ensure DB via DbUtils */ }

    public static SqliteJobRepository instance() {
        if (INSTANCE == null) INSTANCE = new SqliteJobRepository();
        return INSTANCE;
    }

    public static ObjectMapper getMapper() { return mapper; }

    @Override
    public void addJob(Job j) throws Exception {
        try (Connection c = DbUtils.getConnection()) {
            String sql = "INSERT INTO jobs(id,command,state,attempts,max_retries,created_at,updated_at,next_run_at,last_error,worker_id) VALUES(?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, j.getId());
                ps.setString(2, j.getCommand());
                ps.setString(3, j.getState().name());
                ps.setInt(4, j.getAttempts());
                ps.setInt(5, j.getMaxRetries());
                ps.setString(6, j.getCreatedAt().toString());
                ps.setString(7, j.getUpdatedAt().toString());
                ps.setString(8, j.getNextRunAt() == null ? null : j.getNextRunAt().toString());
                ps.setString(9, j.getLastError());
                ps.setString(10, j.getWorkerId());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public List<Job> listAll() throws Exception {
        List<Job> out = new ArrayList<>();
        try (Connection c = DbUtils.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM jobs")) {
            while (rs.next()) out.add(rowToJob(rs));
        }
        return out;
    }

    @Override
    public List<Job> listByState(JobState state) throws Exception {
        List<Job> out = new ArrayList<>();
        String sql = "SELECT * FROM jobs WHERE state = ?";
        try (Connection c = DbUtils.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, state.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rowToJob(rs));
            }
        }
        return out;
    }

   @Override
public Optional<Job> claimNextDueJob(String workerId) throws Exception {
    // Begin transaction to atomically pick and claim
    try (Connection c = DbUtils.getConnection()) {
        c.setAutoCommit(false);
        Job found = null;
        String selectSql = "SELECT * FROM jobs WHERE state = ? AND (next_run_at IS NULL OR next_run_at <= ?) ORDER BY created_at LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(selectSql)) {
            ps.setString(1, JobState.PENDING.name());
            ps.setString(2, Instant.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) found = rowToJob(rs);
            }
        }
        if (found == null) {
            c.commit();
            return Optional.empty();
        }
        // attempt to mark processing and increment attempts atomically
        String updateSql = "UPDATE jobs SET state = ?, attempts = attempts + 1, worker_id = ?, updated_at = ? WHERE id = ? AND state = ?";
        try (PreparedStatement ups = c.prepareStatement(updateSql)) {
            ups.setString(1, JobState.PROCESSING.name());
            ups.setString(2, workerId);
            ups.setString(3, Instant.now().toString());
            ups.setString(4, found.getId());
            ups.setString(5, JobState.PENDING.name());
            int changed = ups.executeUpdate();
            if (changed == 1) {
                // load updated row
                String reload = "SELECT * FROM jobs WHERE id = ?";
                try (PreparedStatement rps = c.prepareStatement(reload)) {
                    rps.setString(1, found.getId());
                    try (ResultSet rs = rps.executeQuery()) {
                        if (rs.next()) {
                            Job j = rowToJob(rs);
                            c.commit();
                            return Optional.of(j);
                        }
                    }
                }
            }
        }
        c.commit();
        return Optional.empty();
    } catch (SQLException ex) {
        throw ex;
    }
}

    @Override
    public void markCompleted(String jobId) throws Exception {
        String sql = "UPDATE jobs SET state = ?, worker_id = NULL, updated_at = ? WHERE id = ?";
        try (Connection c = DbUtils.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, JobState.COMPLETED.name());
            ps.setString(2, Instant.now().toString());
            ps.setString(3, jobId);
            ps.executeUpdate();
        }
    }

    @Override
    public void scheduleRetry(Job job, long delaySeconds, String error) throws Exception {
        String sql = "UPDATE jobs SET state = ?, worker_id = NULL, last_error = ?, next_run_at = ?, updated_at = ? WHERE id = ?";
        try (Connection c = DbUtils.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, JobState.PENDING.name());
            ps.setString(2, error);
            ps.setString(3, Instant.now().plusSeconds(delaySeconds).toString());
            ps.setString(4, Instant.now().toString());
            ps.setString(5, job.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void moveToDlq(Job job, String error) throws Exception {
        String sql = "UPDATE jobs SET state = ?, last_error = ?, worker_id = NULL, updated_at = ? WHERE id = ?";
        try (Connection c = DbUtils.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, JobState.DEAD.name());
            ps.setString(2, error);
            ps.setString(3, Instant.now().toString());
            ps.setString(4, job.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public boolean retryFromDlq(String jobId) throws Exception {
        String sql = "UPDATE jobs SET state = ?, attempts = 0, next_run_at = ?, updated_at = ? WHERE id = ? AND state = ?";
        try (Connection c = DbUtils.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, JobState.PENDING.name());
            ps.setString(2, Instant.now().toString());
            ps.setString(3, Instant.now().toString());
            ps.setString(4, jobId);
            ps.setString(5, JobState.DEAD.name());
            int changed = ps.executeUpdate();
            return changed > 0;
        }
    }

    @Override
    public Map<String, String> getConfig() throws Exception {
        Map<String, String> out = new HashMap<>();
        try (Connection c = DbUtils.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT key, value FROM config")) {
            while (rs.next()) out.put(rs.getString(1), rs.getString(2));
        }
        return out;
    }

    @Override
    public void setConfig(String key, String value) throws Exception {
        String upsert = "INSERT INTO config(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection c = DbUtils.getConnection();
             PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    private Job rowToJob(ResultSet rs) throws SQLException {
        Job j = new Job();
        j.setId(rs.getString("id"));
        j.setCommand(rs.getString("command"));
        j.setState(JobState.valueOf(rs.getString("state")));
        j.setAttempts(rs.getInt("attempts"));
        j.setMaxRetries(rs.getInt("max_retries"));
        j.setCreatedAt(Instant.parse(rs.getString("created_at")));
        j.setUpdatedAt(Instant.parse(rs.getString("updated_at")));
        String next = rs.getString("next_run_at");
        j.setNextRunAt(next == null ? null : Instant.parse(next));
        j.setLastError(rs.getString("last_error"));
        j.setWorkerId(rs.getString("worker_id"));
        return j;
    }
}
