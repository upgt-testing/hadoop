package org.apache.hadoop.hdfs.remoteProxies;

public interface CachingStrategyInterface {
    CachingStrategyInterface newDropBehind();
    java.lang.Long getReadahead();
    java.lang.Boolean getDropBehind();
    java.lang.String toString();
    CachingStrategyInterface newDefaultStrategy();
}