package org.apache.hadoop.metrics2.source;

import org.apache.hadoop.metrics2.MetricsSourceJVMInterface;

public interface JvmMetricsJVMInterface extends MetricsSourceJVMInterface {

    void getMetrics_bridge(java.lang.Object arg0, boolean arg1);

    void setGcTimeMonitor_bridge(org.apache.hadoop.util.GcTimeMonitorJVMInterface arg0);

    void setPauseMonitor_bridge(org.apache.hadoop.util.JvmPauseMonitorJVMInterface arg0);

    void registerIfNeeded();
}
