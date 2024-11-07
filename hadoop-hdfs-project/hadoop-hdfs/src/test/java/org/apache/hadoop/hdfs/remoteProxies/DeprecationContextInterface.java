package org.apache.hadoop.hdfs.remoteProxies;

public interface DeprecationContextInterface {
//    java.util.Map<java.lang.String, org.apache.hadoop.conf.Configuration.DeprecatedKeyInfo> getDeprecatedKeyMap();
    java.util.Map<java.lang.String, java.lang.String> getReverseDeprecatedKeyMap();
}