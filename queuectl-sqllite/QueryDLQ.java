import java.sql.*;
public class QueryDLQ {
  public static void main(String[] args) throws Exception {
    String db = "data\\queue.db";
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db);
         Statement s = c.createStatement();
         ResultSet r = s.executeQuery("SELECT id, state, attempts, max_retries, last_error, command, updated_at FROM jobs WHERE state='DEAD';")) {
      boolean found = false;
      while (r.next()) {
        found = true;
        System.out.println("id=" + r.getString("id"));
        System.out.println("state=" + r.getString("state"));
        System.out.println("attempts=" + r.getInt("attempts"));
        System.out.println("max_retries=" + r.getInt("max_retries"));
        System.out.println("last_error=" + r.getString("last_error"));
        System.out.println("command=" + r.getString("command"));
        System.out.println("updated_at=" + r.getString("updated_at"));
        System.out.println("----------------------------------------------------");
      }
      if (!found) System.out.println("No DEAD jobs found.");
    }
  }
}