package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;

import java.util.*;
import java.io.*;

public interface DatanodeAdminManagerInterface {

    void startDecommission(DatanodeDescriptor node);

    void stopDecommission(DatanodeDescriptor node);

    void startMaintenance(DatanodeDescriptor node, long maintenanceExpireTimeInMS);

    void stopMaintenance(DatanodeDescriptor node);

    int getNumPendingNodes();

    int getNumTrackedNodes();

    int getNumNodesChecked();

    QueueInterface getPendingNodes();
}
