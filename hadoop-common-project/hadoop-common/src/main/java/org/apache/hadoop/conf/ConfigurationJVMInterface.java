package org.apache.hadoop.conf;

import java.util.Map;

public interface ConfigurationJVMInterface {
    Map<String, String> getSetParameters();
    void set(String name, String value);
    String get(String name);
    void setAllParameters(Map<String, String> parameters);
    int getInt(String name, int defaultValue);
    Class<?> getClass(String name, Class<?> defaultValue);
    void setClass(String name, Class<?> theClass, Class<?> xface);
    int[] getInts(String name);
    boolean getBoolean(String name, boolean defaultValue);
}
