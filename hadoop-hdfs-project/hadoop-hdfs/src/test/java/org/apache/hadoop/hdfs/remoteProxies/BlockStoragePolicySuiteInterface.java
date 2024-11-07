package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockStoragePolicySuiteInterface {
    BlockStoragePolicyInterface[] getAllPolicies();
    java.lang.String getStoragePolicyXAttrPrefixedName();
    XAttrInterface buildXAttr(byte arg0);
    boolean isStoragePolicyXAttr(XAttrInterface arg0);
    BlockStoragePolicyInterface getPolicy(byte arg0);
    BlockStoragePolicySuiteInterface createDefaultSuite();
    BlockStoragePolicyInterface getPolicy(java.lang.String arg0);
    java.lang.String buildXAttrName();
    BlockStoragePolicyInterface getDefaultPolicy();
}