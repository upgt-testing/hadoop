package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;

import java.util.*;
import java.io.*;

public interface BlockReportLeaseManagerInterface {

    void register(DatanodeDescriptor dn);

    void unregister(DatanodeDescriptor dn);

    long requestLease(DatanodeDescriptor dn);

    boolean checkLease(DatanodeDescriptor dn, long monotonicNowMs, long id);

    long removeLease(DatanodeDescriptor dn);
}
