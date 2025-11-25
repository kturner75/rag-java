package com.nmthome;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

public class SakilaChatApp {
  private static JTextArea chatArea;
  private static JTextField inputField;
  private static JScrollPane scrollPane;

  public static void main(String[] args) {
    SwingUtilities.invokeLater(SakilaChatApp::createAndShowGUI);
  }

  private static void createAndShowGUI() {
    JFrame frame = new JFrame("Sakila Assistant (Local LLM + MariaDB)");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(900, 700);
    frame.setLocationRelativeTo(null);

    // === Chat display area ===
    chatArea = new JTextArea();
    chatArea.setEditable(false);
    chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
    chatArea.setLineWrap(true);
    chatArea.setWrapStyleWord(true);
    chatArea.setMargin(new Insets(15, 15, 15, 15));

    scrollPane = new JScrollPane(chatArea);
    DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // auto-scroll

    // === Input panel ===
    JPanel inputPanel = new JPanel(new BorderLayout());
    inputField = new JTextField();
    inputField.setFont(new Font("Segoe UI", Font.PLAIN, 16));

    JButton sendButton = new JButton("Send");
    sendButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
    sendButton.setBackground(new Color(0, 122, 255));
    sendButton.setForeground(Color.WHITE);
    sendButton.setFocusPainted(false);

    inputPanel.add(inputField, BorderLayout.CENTER);
    inputPanel.add(sendButton, BorderLayout.EAST);
    inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // === Layout ===
    frame.add(scrollPane, BorderLayout.CENTER);
    frame.add(inputPanel, BorderLayout.SOUTH);

    // === Welcome message ===
    appendMessage("Sakila Assistant", "Hello! I'm your private data analyst.\n" +
        "Ask me anything about the Sakila database — in plain English! 🚀\n\n", Color.BLUE);

    // === Actions ===
    sendButton.addActionListener(e -> sendMessage());
    inputField.addActionListener(e -> sendMessage());

    // Press Enter = send, Shift+Enter = new line
    inputField.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER && !evt.isShiftDown()) {
          evt.consume();
          sendMessage();
        }
      }
    });

    frame.setVisible(true);
    inputField.requestFocus();
  }

  private static void sendMessage() {
    String question = inputField.getText().trim();
    if (question.isEmpty()) return;

    appendMessage("You", question + "\n", new Color(0, 100, 0));
    inputField.setText("");
    inputField.setEnabled(false);

    // Run LLM + DB in background so UI doesn't freeze
    SwingWorker<String, Void> worker = new SwingWorker<>() {
      @Override
      protected String doInBackground() {
        try {
          return SakilaChat.askQuestion(question);
        } catch (SQLException ex) {
          return "Database error: " + ex.getMessage();
        }
      }

      @Override
      protected void done() {
        String answer;
        try {
          answer = get();
        } catch (Exception ex) {
          answer = "Something went wrong: " + ex.getMessage();
        }
        appendMessage("Sakila Assistant", answer + "\n\n", new Color(0, 70, 150));
        inputField.setEnabled(true);
        inputField.requestFocus();
      }
    };
    worker.execute();
  }

  private static void appendMessage(String sender, String text, Color color) {
    chatArea.append("╭─ " + sender + "\n");
    chatArea.append("│ ");
    chatArea.setForeground(color);
    chatArea.append(text);
    chatArea.setForeground(Color.BLACK);
  }
}