package com.nmthome;

import static com.nmthome.SakilaChat.normalizeQuestion;

public class SqlGenerator {

  private static final String SYNONYM_HEADER = """
          Note: In this database, "movie" = "film", "customer who rented" = "customer with rental records",
          "revenue/sales/money made" = sum of payment.amount, "most popular" = highest rental count.
          Treat these as identical.
      
      """;
  private static final String SAKILA_PROMPT = """
      You are an expert MariaDB analyst for the Sakila database.
      Use ONLY the exact tables and columns below — never guess or hallucinate names.
      
      EXACT DATABASE SCHEMA:
      %s
      
      RULES:
      • Use backticks around column/table names only if necessary (MariaDB is forgiving)
      • All money is in payment.amount (decimal)
      • Dates are in rental.rental_date, payment.payment_date, etc.
      • Return ONLY a single ```sql code block. No explanations, no markdown outside the block.
      
      User question: %s
      
      SQL:```sql
      """;

  public static String generateSql(String userQuestion) {
    String normalized = normalizeQuestion(userQuestion);
    String prompt = SYNONYM_HEADER + SAKILA_PROMPT.formatted(SchemaLoader.getSchema(), normalized);
    String raw = OllamaClient.ask(prompt, "llama3.1:8b");
    return extractSql(raw);
  }

  private static String extractSql(String text) {
    if (text == null || text.isBlank()) return null;

    // Step 1: Try to find ```sql ... ```
    int sqlStart = text.toLowerCase().indexOf("```sql");
    if (sqlStart == -1) {
      // Step 2: Fall back to any ```
      sqlStart = text.indexOf("```");
      if (sqlStart == -1) {
        // Step 3: Absolute fallback — find first SELECT
        int selectPos = text.toLowerCase().indexOf("select ");
        if (selectPos == -1) return null;
        String candidate = text.substring(selectPos);
        int semicolon = candidate.indexOf(";");
        if (semicolon != -1) candidate = candidate.substring(0, semicolon + 1);
        return cleanSql(candidate);
      }
      sqlStart += 3; // skip the opening ```
    } else {
      sqlStart += 6; // skip "```sql"
    }

    // Now find the closing ```
    int sqlEnd = text.indexOf("```", sqlStart);
    if (sqlEnd == -1) sqlEnd = text.length();

    String sql = text.substring(sqlStart, sqlEnd).trim();

    // Clean any leftover junk the model loves to add
    sql = sql.replaceAll("(?i)^sql:?\\s*", "");     // removes "sql:" or "SQL:"
    sql = sql.replaceAll("^--.*$", "");            // remove comment lines
    sql = sql.lines()
        .map(String::trim)
        .filter(line -> !line.isEmpty() && !line.startsWith("--"))
        .collect(java.util.stream.Collectors.joining(" "))
        .trim();

    return (sql.toLowerCase().startsWith("select")) ? sql : null;
  }

  private static String cleanSql(String sql) {
    return sql.replaceFirst("^(sql|SQL|Sql)?[:\\s-]*", "")
        .lines()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .collect(java.util.stream.Collectors.joining(" "))
        .trim();
  }
}