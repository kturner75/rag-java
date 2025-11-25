package com.nmthome;

import java.sql.*;
import java.util.StringJoiner;

public class SakilaChat {

  private static final String JDBC_URL = "jdbc:mariadb://localhost:3306/sakila";
  private static final String USER = "root";        // ← change if needed
  private static final String PASS = "cft6yhn"; // ← change!

  public static String askQuestion(String question) throws SQLException {
    // Step 1: Generate SQL
    String sql = SqlGenerator.generateSql(question);
    if (sql == null) {
      return "I couldn't generate valid SQL for that question.";
    }

    // Step 2: Execute safely
    String markdownTable = executeSql(sql);
    if (markdownTable == null) {
      return "The query ran but returned no data.";
    }

    // Step 3: Ask Ollama to explain the results naturally
    String explanationPrompt = """
            You are a friendly data analyst.
            The user asked: "%s"
            
            Here are the query results (first rows shown):
            
            %s
            
            Give a clear, concise, natural-language answer.
            Include key numbers and insights.
            """.formatted(question, markdownTable);

    return OllamaClient.ask(explanationPrompt, "llama3.1:8b");
  }

  private static String executeSql(String sql) throws SQLException {
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
    }
  }
}