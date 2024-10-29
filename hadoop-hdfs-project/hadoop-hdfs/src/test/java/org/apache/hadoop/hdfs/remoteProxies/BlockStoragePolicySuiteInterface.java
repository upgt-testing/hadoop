package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.BlockStoragePolicy;

public interface BlockStoragePolicySuiteInterface {

    BlockStoragePolicyInterface getPolicy(byte id);

    BlockStoragePolicy getDefaultPolicy();

    BlockStoragePolicy getPolicy(String policyName);

    BlockStoragePolicy[] getAllPolicies();
}
