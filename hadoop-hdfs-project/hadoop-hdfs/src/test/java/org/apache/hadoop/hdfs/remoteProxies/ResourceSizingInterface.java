package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.service.api.records.Resource;

public interface ResourceSizingInterface {

    int getNumAllocations();

    void setNumAllocations(int numAllocations);

    ResourceInterface getResources();

    //void setResources(Resource resources);

    int hashCode();

    boolean equals(Object obj);
}
