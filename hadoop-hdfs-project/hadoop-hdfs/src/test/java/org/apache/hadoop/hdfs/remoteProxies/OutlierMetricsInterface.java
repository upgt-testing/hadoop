package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface OutlierMetricsInterface {

    Double getMedian();

    Double getMad();

    Double getUpperLimitLatency();

    Double getActualLatency();

    boolean equals(Object o);

    int hashCode();
}
