package org.apache.hadoop.hdfs.remoteProxies;


public interface NameNodeProxy {
    FSNameSystemProxy getNamesystem() throws Exception;
    boolean testRMIPrint(String message);
    void testRMIConf(org.apache.hadoop.conf.Configuration conf);
}
