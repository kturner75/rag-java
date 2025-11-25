package com.nmthome;

import java.sql.*;
import java.util.StringJoiner;

public class SakilaChat {

  private static final String JDBC_URL = "jdbc:mariadb://localhost:3306/sakila";
  private static final String USER = "root";        // ← change if needed
  private static final String PASS = "cft6yhn"; // ← change!

  // Inside SakilaChat.java — replace the old askQuestion method
  public static String askQuestion(String question) throws SQLException {
    String sql;
    String lastError = null;

    for (int attempt = 1; attempt <= 3; attempt++) {
      sql = SqlGenerator.generateSql(
          attempt == 1 ?
              question :
              question + "\n\nPrevious SQL failed with error: " + lastError +
                  "\nFix it and return ONLY the corrected ```sql block."
      );

      if (sql == null) {
        if (attempt == 3) return "I tried 3 times but couldn't generate valid SQL. Try rephrasing your question.";
        continue;
      }

      String result = executeSqlSafely(sql);
      if (result != null) {
        // Success! → explain results
        String explanationPrompt = """
            You are a friendly data analyst.
            User question: "%s"
            Query results (first rows):
            
            %s
            
            Give a clear, insightful answer in plain English.
            """.formatted(question, result);

        return OllamaClient.ask(explanationPrompt, "llama3.1:8b");
      }

      // Failed → capture error for next attempt
      lastError = executeSqlSafely(sql); // returns error message if null result
      if (lastError == null) lastError = "Unknown database error";
    }

    return "I couldn't execute a working query after 3 attempts. The last error was: " + lastError;
  }

  static String normalizeQuestion(String question) {
    // Sakila-specific synonyms and common-sense mappings
    return question.toLowerCase()
        .replace("movies", "films")
        .replace("movie", "film")
        .replace("dramas", "drama")           // category is exactly "Drama"
        .replace("comedies", "comedy")
        .replace("horror films", "films in the Horror category")
        .replace("action films", "films in the Action category")
        .replace("revenue", "payment amounts")
        .replace("sales", "payment amounts")
        .replace("customers who rented", "customers with rentals")
        .replace("most rented", "with the highest rental count")
        .replace("biggest spender", "highest total payment amount")
        .replace("best customer", "customer with highest total payments")
        .replace("top customer", "customer with highest total payments")
        // Make the first letter uppercase so the prompt reads naturally
        .replaceFirst("^.", String.valueOf(Character.toUpperCase(question.charAt(0))));
  }

  private static String executeSqlSafely(String sql) {
    if (!isSafeSql(sql)) {
      return "SECURITY BLOCK: This query contains dangerous keywords (DROP, DELETE, etc.) and was blocked.";
    }

    try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASS);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

      ResultSetMetaData meta = rs.getMetaData();
      int cols = meta.getColumnCount();

      // Build Markdown table
      StringJoiner header = new StringJoiner(" | ", "| ", " |");
      StringJoiner separator = new StringJoiner(" | ", "| ", " |");
      for (int i = 1; i <= cols; i++) {
        header.add(meta.getColumnLabel(i));
        separator.add("---");
      }

      StringBuilder table = new StringBuilder();
      table.append(header).append("\n").append(separator).append("\n");

      int rowCount = 0;
      while (rs.next() && rowCount < 20) {  // limit for prompt size
        StringJoiner row = new StringJoiner(" | ", "| ", " |");
        for (int i = 1; i <= cols; i++) {
          Object val = rs.getObject(i);
          row.add(val == null ? "NULL" : val.toString());
        }
        table.append(row).append("\n");
        rowCount++;
      }

      if (!rs.isLast() && rowCount == 20) {
        table.append("_... (showing first 20 rows)_");
      }

      return table.toString();

    } catch (SQLException e) {
      return e.getMessage();  // this becomes lastError for next attempt
    }
  }

  private static boolean isSafeSql(String sql) {
    String lower = sql.toLowerCase();
    String[] dangerous = {"drop ", "delete ", "insert ", "update ", "create ", "alter ", "truncate ", "grant ", "revoke "};
    for (String kw : dangerous) {
      if (lower.contains(kw)) return false;
    }
    return true;
  }
}