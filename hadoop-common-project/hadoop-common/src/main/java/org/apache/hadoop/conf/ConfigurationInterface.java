package org.apache.hadoop.conf;

import java.util.Map;

public interface ConfigurationInterface {
    void set(String name, String value);
    String get(String name);
    void setAllParameters(Map<String, String> parameters);
}
