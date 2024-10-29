package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface NumberReplicasInterface {

    int liveReplicas();

    int readOnlyReplicas();

    int decommissionedAndDecommissioning();

    int decommissioned();

    int decommissioning();

    int corruptReplicas();

    int excessReplicas();

    int replicasOnStaleNodes();

    int redundantInternalBlocks();

    int maintenanceNotForReadReplicas();

    int maintenanceReplicas();

    int outOfServiceReplicas();

    int liveEnteringMaintenanceReplicas();
}
