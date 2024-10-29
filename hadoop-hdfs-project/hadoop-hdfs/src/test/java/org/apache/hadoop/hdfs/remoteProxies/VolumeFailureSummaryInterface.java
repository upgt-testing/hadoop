package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface VolumeFailureSummaryInterface {

    String[] getFailedStorageLocations();

    long getLastVolumeFailureDate();

    long getEstimatedCapacityLostTotal();
}
