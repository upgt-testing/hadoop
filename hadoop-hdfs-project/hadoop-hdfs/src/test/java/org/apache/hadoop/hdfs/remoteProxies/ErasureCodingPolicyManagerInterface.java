package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicyInfo;

public interface ErasureCodingPolicyManagerInterface {

    void init(Configuration conf);

    ErasureCodingPolicyInterface[] getEnabledPolicies();

    ErasureCodingPolicyInterface getEnabledPolicyByName(String name);

    ErasureCodingPolicyInfoInterface[] getPolicies();

    ErasureCodingPolicyInfoInterface[] getPersistedPolicies();

    ErasureCodingPolicyInterface[] getCopyOfEnabledPolicies();

    ErasureCodingPolicyInterface getByID(byte id);

    ErasureCodingPolicyInterface getByName(String name);

    ErasureCodingPolicyInterface getErasureCodingPolicyByName(String name);

    void clear();

    ErasureCodingPolicyInterface addPolicy(ErasureCodingPolicy policy);

    void removePolicy(String name);

    List<ErasureCodingPolicyInterface> getRemovedPolicies();

    boolean disablePolicy(String name);

    boolean enablePolicy(String name);

    void loadPolicies(List<ErasureCodingPolicyInfo> ecPolicies, Configuration conf);

    String getEnabledPoliciesMetric();
}
