package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.net.Node;

public interface DatanodeInfoInterface {

    String getName();

    long getCapacity();

    long getDfsUsed();

    long getBlockPoolUsed();

    long getNonDfsUsed();

    float getDfsUsedPercent();

    long getRemaining();

    float getBlockPoolUsedPercent();

    float getRemainingPercent();

    long getCacheCapacity();

    long getCacheUsed();

    float getCacheUsedPercent();

    long getCacheRemaining();

    float getCacheRemainingPercent();

    long getLastUpdate();

    long getLastUpdateMonotonic();

    int getNumBlocks();

    void setLastUpdateMonotonic(long lastUpdateMonotonic);

    int getXceiverCount();

    void setCapacity(long capacity);

    void setDfsUsed(long dfsUsed);

    void setNonDfsUsed(long nonDfsUsed);

    void setRemaining(long remaining);

    void setBlockPoolUsed(long bpUsed);

    void setCacheCapacity(long cacheCapacity);

    void setCacheUsed(long cacheUsed);

    void setLastUpdate(long lastUpdate);

    void setXceiverCount(int xceiverCount);

    void setNumBlocks(int blockCount);

    String getNetworkLocation();

    void setNetworkLocation(String location);

    void setUpgradeDomain(String upgradeDomain);

    String getUpgradeDomain();

    void addDependentHostName(String hostname);

    List<String> getDependentHostNames();

    void setDependentHostNames(List<String> dependencyList);

    String getDatanodeReport();

    String dumpDatanode();

    void startDecommission();

    void stopDecommission();

    boolean isDecommissionInProgress();

    boolean isDecommissioned();

    void setDecommissioned();

    void startMaintenance();

    void setInMaintenance();

    void setMaintenanceExpireTimeInMS(long maintenanceExpireTimeInMS);

    long getMaintenanceExpireTimeInMS();

    void setLastBlockReportTime(long lastBlockReportTime);

    void setLastBlockReportMonotonic(long lastBlockReportMonotonic);

    long getLastBlockReportTime();

    long getLastBlockReportMonotonic();

    void stopMaintenance();

    boolean isEnteringMaintenance();

    boolean isInMaintenance();

    boolean isMaintenance();

    boolean maintenanceExpired();

    boolean isInService();

    //AdminStates getAdminState();

    boolean isStale(long staleInterval);

    Node getParent();

    void setParent(Node parent);

    int getLevel();

    void setLevel(int level);

    int hashCode();

    boolean equals(Object obj);

    String getSoftwareVersion();

    void setSoftwareVersion(String softwareVersion);
}
