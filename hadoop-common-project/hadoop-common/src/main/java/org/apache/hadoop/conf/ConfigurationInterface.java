package org.apache.hadoop.conf;

public interface ConfigurationInterface {
    void set(String name, String value);
    String get(String name);
}
