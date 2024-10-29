package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ErasureCodingPolicyInterface {

    String getName();

    ECSchemaInterface getSchema();

    int getCellSize();

    int getNumDataUnits();

    int getNumParityUnits();

    String getCodecName();

    byte getId();

    boolean isReplicationPolicy();

    boolean isSystemPolicy();

    boolean equals(Object o);

    int hashCode();

    String toString();
}
