package com.nmthome;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {

  private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
  private static final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();

  private static final ObjectMapper mapper = new ObjectMapper();

  public static String ask(String prompt, String model) {
    try {
      ObjectNode json = mapper.createObjectNode();
      json.put("model", model);
      json.put("prompt", prompt);
      json.put("stream", false);

      String payload = mapper.writeValueAsString(json);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(OLLAMA_URL))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(payload))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        return "HTTP " + response.statusCode() + ": " + response.body();
      }

      return mapper.readTree(response.body())
          .get("response")
          .asText();

    } catch (Exception e) {
      return "Exception: " + e.getMessage();
    }
  }

  public static String ask(String prompt) {
    return ask(prompt, "llama3.1:8b");
  }
}