package org.apache.hadoop.hdfs.server.process.upgrade;

import org.junit.Test;

public class TestDiagnostic {
  @Test
  public void printEnvironment() {
    System.out.println("=== ENVIRONMENT VARIABLES ===");
    System.out.println("HADOOP_HOME (env): " + System.getenv("HADOOP_HOME"));
    System.out.println("HADOOP_3_3_5_HOME (env): " + System.getenv("HADOOP_3_3_5_HOME"));
    System.out.println("HADOOP_3_3_6_HOME (env): " + System.getenv("HADOOP_3_3_6_HOME"));

    System.out.println("\n=== SYSTEM PROPERTIES ===");
    System.out.println("HADOOP_HOME (prop): " + System.getProperty("HADOOP_HOME"));
    System.out.println("HADOOP_3_3_5_HOME (prop): " + System.getProperty("HADOOP_3_3_5_HOME"));
    System.out.println("HADOOP_3_3_6_HOME (prop): " + System.getProperty("HADOOP_3_3_6_HOME"));

    System.out.println("\n=== USER DIR ===");
    System.out.println("user.dir: " + System.getProperty("user.dir"));
  }
}
