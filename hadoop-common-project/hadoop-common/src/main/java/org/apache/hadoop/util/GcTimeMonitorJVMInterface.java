package org.apache.hadoop.util;

public interface GcTimeMonitorJVMInterface {

    java.lang.Object getLatestGcData();

    void run();

    void shutdown();
}
