package org.apache.hadoop.ipc;

import org.apache.hadoop.ipc.metrics.RetryCacheMetricsJVMInterface;

public interface RetryCacheJVMInterface {
    RetryCacheMetricsJVMInterface getMetricsForTests();
}
