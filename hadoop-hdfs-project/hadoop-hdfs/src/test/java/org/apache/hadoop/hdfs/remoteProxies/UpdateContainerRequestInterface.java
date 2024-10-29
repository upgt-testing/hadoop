package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface UpdateContainerRequestInterface {

    int getContainerVersion();

    void setContainerVersion(int containerVersion);

    //ContainerUpdateType getContainerUpdateType();

    //void setContainerUpdateType(ContainerUpdateType updateType);

    ContainerIdInterface getContainerId();

    //void setContainerId(ContainerId containerId);

    //ExecutionType getExecutionType();

    //void setExecutionType(ExecutionType executionType);

    //void setCapability(Resource capability);

    //Resource getCapability();

    int hashCode();

    String toString();

    boolean equals(Object obj);
}
