package com.nmthome;

import java.sql.SQLException;

public class Main {
  public static void main(String[] args) throws SQLException {
    String[] questions = {
        "How many films are in the Comedy category?",
        "Who are the top 5 actors by number of films?",
        "What was total revenue in June 2005?",
        "List the 10 most rented films",
        "Which store has the highest revenue?",
        "Which customer has spent the most money lifetime?"
    };

    for (String q : questions) {
      System.out.println("\n" + "=".repeat(80));
      System.out.println("QUESTION: " + q);
      System.out.println("=".repeat(80));
      String answer = SakilaChat.askQuestion(q);
      System.out.println(answer);
    }
  }
}