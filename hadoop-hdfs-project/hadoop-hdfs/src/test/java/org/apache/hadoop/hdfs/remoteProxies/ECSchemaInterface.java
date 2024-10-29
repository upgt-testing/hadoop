package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ECSchemaInterface {

    String getCodecName();

    Map<String, String> getExtraOptions();

    int getNumDataUnits();

    int getNumParityUnits();

    String toString();

    boolean equals(Object o);

    int hashCode();
}
