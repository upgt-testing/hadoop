package org.apache.hadoop.hdfs.remoteProxies;

public interface ErasureCodingPolicyManagerInterface {
    ErasureCodingPolicyInfoInterface getPolicyInfoByName(java.lang.String arg0);
    void updatePolicies();
    ErasureCodingPolicyInterface[] getEnabledPolicies();
    ErasureCodingPolicyInfoInterface[] getPersistedPolicies();
    ErasureCodingPolicyManagerInterface getInstance();
    byte getNextAvailablePolicyID();
    void removePolicy(java.lang.String arg0);
    ErasureCodingPolicyInfoInterface getPolicyInfoByID(byte arg0);
    void clear();
    ErasureCodingPolicyInterface getEnabledPolicyByName(java.lang.String arg0);
    ErasureCodingPolicyInterface[] getCopyOfEnabledPolicies();
    ErasureCodingPolicyInfoInterface createPolicyInfo(ErasureCodingPolicyInterface arg0, org.apache.hadoop.hdfs.protocol.ErasureCodingPolicyState arg1);
    boolean disablePolicy(java.lang.String arg0);
    void loadPolicies(java.util.List<org.apache.hadoop.hdfs.protocol.ErasureCodingPolicyInfo> arg0, ConfigurationInterface arg1) throws java.io.IOException;
    ErasureCodingPolicyInterface addPolicy(ErasureCodingPolicyInterface arg0);
    boolean checkStoragePolicySuitableForECStripedMode(byte arg0);
    void enableDefaultPolicy(ConfigurationInterface arg0) throws java.io.IOException;
    ErasureCodingPolicyInterface getErasureCodingPolicyByName(java.lang.String arg0);
    ErasureCodingPolicyInterface getByName(java.lang.String arg0);
    ErasureCodingPolicyInfoInterface[] getPolicies();
    void init(ConfigurationInterface arg0) throws java.io.IOException;
    boolean enablePolicy(java.lang.String arg0);
    void loadPolicy(ErasureCodingPolicyInfoInterface arg0);
    java.lang.String getEnabledPoliciesMetric();
    java.util.List<org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy> getRemovedPolicies();
    byte getCurrentMaxPolicyID();
    ErasureCodingPolicyInterface getByID(byte arg0);
}