package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockChecksumOptionsInterface {
    org.apache.hadoop.hdfs.protocol.BlockChecksumType getBlockChecksumType();
    long getStripeLength();
    java.lang.String toString();
}