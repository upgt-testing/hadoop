package org.apache.hadoop.fs;

public interface StorageStatisticsJVMInterface {

    java.lang.Long getLong(java.lang.String arg0);

    java.lang.String getScheme();

    java.lang.String getName();

    boolean isTracked(java.lang.String arg0);

    java.util.Iterator getLongStatistics();

    void reset();
}
