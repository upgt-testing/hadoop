package org.apache.hadoop.hdfs.remoteProxies;

public interface DatanodeInfoWithStorageInterface extends DatanodeInfoInterface {
    java.lang.String getXferAddr(boolean arg0);
    boolean equals(java.lang.Object arg0);
    int getInfoPort();
    java.lang.String getIpAddr();
    void setLastUpdate(long arg0);
    boolean isStale(long arg0);
    java.lang.String getIpcAddr(boolean arg0);
    void setCacheUsed(long arg0);
    int compareTo(DatanodeIDInterface arg0);
    org.apache.hadoop.net.Node getParent();
    void setXceiverCount(int arg0);
    void setBlockPoolUsed(long arg0);
    int getXceiverCount();
    int getLevel();
    java.net.InetSocketAddress getResolvedAddress();
    java.util.List<java.lang.String> getDependentHostNames();
    org.apache.hadoop.fs.StorageType getStorageType();
    org.apache.hadoop.hdfs.protocol.DatanodeInfo.AdminStates getAdminState();
    void setLevel(int arg0);
    ByteStringInterface getByteString(java.lang.String arg0);
    void setNumBlocks(int arg0);
    void setNonDfsUsed(long arg0);
    boolean maintenanceNotExpired(long arg0);
    java.lang.String getIpcAddr();
    void setDecommissioned();
    java.lang.String dumpDatanode();
    ByteStringInterface getDatanodeUuidBytes();
    boolean isInService();
    void setPeerHostName(java.lang.String arg0);
    void setAdminState(org.apache.hadoop.hdfs.protocol.DatanodeInfo.AdminStates arg0);
    long getCapacity();
    int getIpcPort();
    boolean isDecommissionInProgress();
    java.lang.String getInfoSecureAddr();
    void setIpAddr(java.lang.String arg0);
    int getNumBlocks();
    void startMaintenance();
    java.lang.String getSoftwareVersion();
    ByteStringInterface getHostNameBytes();
    float getCacheUsedPercent();
    boolean isMaintenance();
    void setDfsUsed(long arg0);
    java.lang.String getXferAddrWithHostname();
    void setRemaining(long arg0);
    void setLastBlockReportTime(long arg0);
    boolean maintenanceExpired();
    java.lang.String getUpgradeDomain();
    long getCacheRemaining();
    void setUpgradeDomain(java.lang.String arg0);
    float getCacheRemainingPercent();
    void setCapacity(long arg0);
    boolean isInMaintenance();
    float getRemainingPercent();
    long getMaintenanceExpireTimeInMS();
    long getLastBlockReportMonotonic();
    float getDfsUsedPercent();
    int hashCode();
    java.lang.String getDatanodeReport();
    long getLastUpdate();
    java.lang.String getName();
    java.lang.String getPeerHostName();
    void setInMaintenance();
    void setParent(org.apache.hadoop.net.Node arg0);
    void addDependentHostName(java.lang.String arg0);
    java.lang.String checkDatanodeUuid(java.lang.String arg0);
    long getDfsUsed();
    java.lang.String getXferAddr();
    void stopDecommission();
    long getLastUpdateMonotonic();
    boolean isDecommissioned();
    int getXferPort();
    void setLastUpdateMonotonic(long arg0);
    long getNonDfsUsed();
    void stopMaintenance();
    java.lang.String getStorageID();
    long getRemaining();
    float getBlockPoolUsedPercent();
    ByteStringInterface getIpAddrBytes();
    java.lang.String toString();
    void setCacheCapacity(long arg0);
    java.lang.String getIpcAddrWithHostname();
    long getCacheCapacity();
    void startDecommission();
    int getInfoSecurePort();
    java.lang.String getInfoAddr();
    java.lang.String getNetworkLocation();
    void setIpAndXferPort(java.lang.String arg0, ByteStringInterface arg1, int arg2);
    java.lang.String getDatanodeUuid();
    void setLastBlockReportMonotonic(long arg0);
    void setNetworkLocation(java.lang.String arg0);
    long getLastBlockReportTime();
    java.lang.String getHostName();
    boolean isEnteringMaintenance();
    void setDependentHostNames(java.util.List<java.lang.String> arg0);
    void setMaintenanceExpireTimeInMS(long arg0);
    long getBlockPoolUsed();
    void updateRegInfo(DatanodeIDInterface arg0);
    void setSoftwareVersion(java.lang.String arg0);
    long getCacheUsed();
}