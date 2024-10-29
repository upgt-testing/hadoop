package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.DatanodeID;

public interface HostConfigManagerInterface {

    //Iterable<InetSocketAddress> getIncludes();

    //Iterable<InetSocketAddress> getExcludes();

    boolean isIncluded(DatanodeID dn);

    boolean isExcluded(DatanodeID dn);

    void refresh();

    String getUpgradeDomain(DatanodeID dn);

    long getMaintenanceExpirationTimeInMS(DatanodeID dn);
}
