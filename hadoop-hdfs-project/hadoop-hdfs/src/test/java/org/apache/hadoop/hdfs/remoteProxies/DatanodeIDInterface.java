package org.apache.hadoop.hdfs.remoteProxies;

public interface DatanodeIDInterface {
    ByteStringInterface getByteString(java.lang.String arg0);
    java.lang.String getInfoSecureAddr();
    java.lang.String getIpcAddr(boolean arg0);
    int getInfoPort();
    java.lang.String getIpcAddr();
    java.lang.String getIpAddr();
    java.lang.String getDatanodeUuid();
    ByteStringInterface getIpAddrBytes();
    java.lang.String getIpcAddrWithHostname();
    ByteStringInterface getDatanodeUuidBytes();
    java.lang.String getXferAddr();
    int hashCode();
    java.net.InetSocketAddress getResolvedAddress();
    int getXferPort();
    void setIpAddr(java.lang.String arg0);
    void updateRegInfo(DatanodeIDInterface arg0);
    int getInfoSecurePort();
    java.lang.String getXferAddrWithHostname();
    java.lang.String getPeerHostName();
    java.lang.String getXferAddr(boolean arg0);
    void setPeerHostName(java.lang.String arg0);
    int compareTo(DatanodeIDInterface arg0);
    int getIpcPort();
    java.lang.String getInfoAddr();
    ByteStringInterface getHostNameBytes();
    java.lang.String getHostName();
    void setIpAndXferPort(java.lang.String arg0, ByteStringInterface arg1, int arg2);
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    java.lang.String checkDatanodeUuid(java.lang.String arg0);
}