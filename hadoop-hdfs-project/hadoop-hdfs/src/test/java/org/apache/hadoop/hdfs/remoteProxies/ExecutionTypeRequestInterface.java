package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ExecutionTypeRequestInterface {

    //void setExecutionType(ExecutionType execType);

    //ExecutionType getExecutionType();

    void setEnforceExecutionType(boolean enforceExecutionType);

    boolean getEnforceExecutionType();

    int hashCode();

    boolean equals(Object obj);
}
