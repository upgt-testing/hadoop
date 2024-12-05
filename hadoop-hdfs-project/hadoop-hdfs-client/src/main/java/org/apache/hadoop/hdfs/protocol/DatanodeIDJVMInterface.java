package org.apache.hadoop.hdfs.protocol;

public interface DatanodeIDJVMInterface {
    String getDatanodeUuid();
    String getXferAddr();
    String getIpAddr();
    String getHostName();
    int getXferPort();
    int getInfoPort();
    int getInfoSecurePort();
    int getIpcPort();
}
