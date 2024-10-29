package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface RollingUpgradeInfoInterface {

    boolean createdRollbackImages();

    void setCreatedRollbackImages(boolean created);

    boolean isStarted();

    long getStartTime();

    boolean isFinalized();

    void finalize(long finalizeTime);

    long getFinalizeTime();

    int hashCode();

    boolean equals(Object obj);

    String toString();
}
