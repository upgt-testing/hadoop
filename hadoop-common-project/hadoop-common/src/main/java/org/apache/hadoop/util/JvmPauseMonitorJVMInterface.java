package org.apache.hadoop.util;

import org.apache.hadoop.service.AbstractServiceJVMInterface;

public interface JvmPauseMonitorJVMInterface extends AbstractServiceJVMInterface {

    long getTotalGcExtraSleepTime();

    long getNumGcWarnThresholdExceeded();

    long getNumGcInfoThresholdExceeded();

    boolean isStarted();
}
