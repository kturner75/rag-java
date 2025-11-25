package com.nmthome;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SchemaLoader {
  private static final String SCHEMA;

  static {
    try {
      SCHEMA = Files.readString(Paths.get("sakila_schema.sql"));
    } catch (Exception e) {
      throw new RuntimeException("Could not load sakila_schema.sql — place it in project root", e);
    }
  }

  public static String getSchema() {
    return SCHEMA;
  }
}