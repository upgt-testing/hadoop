package org.apache.hadoop.hdfs.protocol;

public interface DatanodeInfoJVMInterface {
    void startDecommission();
    String getName();
    void setLastUpdate(long lastUpdate);
    void setLastUpdateMonotonic(long lastUpdateMonotonic);
    void setUpgradeDomain(String upgradeDomain);
    String getHostName();
    String getNetworkLocation();
    void setDecommissioned();
}
