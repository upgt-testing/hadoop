package org.apache.hadoop.hdfs;


import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.hdfs.protocol.RollingUpgradeInfo;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection;
import org.apache.hadoop.hdfs.server.namenode.CacheManager;
import org.apache.hadoop.hdfs.server.namenode.FSDirectory;
import org.apache.hadoop.hdfs.server.namenode.NameNodeMXBean;
import org.apache.hadoop.hdfs.server.namenode.Namesystem;
import org.apache.hadoop.hdfs.server.namenode.ha.HAContext;
import org.apache.hadoop.hdfs.server.namenode.metrics.ECBlockGroupsMBean;
import org.apache.hadoop.hdfs.server.namenode.metrics.FSNamesystemMBean;
import org.apache.hadoop.hdfs.server.namenode.metrics.ReplicatedBlocksMBean;
import org.apache.hadoop.metrics2.annotation.Metrics;


import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@InterfaceAudience.Private
@Metrics(context="dfs")
public class FSNamesystemProxy implements Namesystem, FSNamesystemMBean,
        NameNodeMXBean, ReplicatedBlocksMBean, ECBlockGroupsMBean {

    public FSNamesystemProxy(String host, int port) {
        connectMXBean(host, port);
    }

    private MBeanServerConnection mbs;
    private ObjectName mxbeanName;

    public MBeanServerConnection getMBeanServer() {
        return mbs;
    }

    public ObjectName getMXBeanName() {
        return mxbeanName;
    }

    private void connectMXBean(String host, int port) {
        //String host = "localhost"; // Updated to 'localhost'
        //int port = 9090;

        // Create a JMX service URL
        String urlString = String.format(
                "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi",
                host, port
        );
        try {
            JMXServiceURL serviceURL = new JMXServiceURL(urlString);
            Map<String, Object> env = new HashMap<>();

            // Connect to the JMX server
            JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
            mbs = jmxConnector.getMBeanServerConnection();
            // this is registered with FSNamesystemMBean
            mxbeanName = new ObjectName(
                    "Hadoop:service=NameNode,name=FSNamesystemState");
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to the JMX server in FSNamesystemState", e);
        }
    }

    private Object invokeRMI(String operationName, Object[] params, String[] signature) {
        try {
            return mbs.invoke(mxbeanName, operationName, params, signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke RMI operation in FSNamesystemState", e);
        }
    }

    public void fakePrintMXBean() {
        invokeRMI("fakePrintMXBean", new Object[]{}, new String[]{});
    }

    @Override
    public void enterSafeMode(boolean resourcesLow) throws IOException{
        invokeRMI("enterSafeMode", new Object[]{resourcesLow}, new String[]{boolean.class.getName()});
    }

    @Override
    public boolean saveNamespace(long txid, long imageTxId) throws IOException {
        return (boolean) invokeRMI("saveNamespace", new Object[]{txid, imageTxId}, new String[]{long.class.getName(), long.class.getName()});
    }

    @Override
    public String getVersion() {
        return (String) invokeRMI("getVersion", new Object[]{}, new String[]{});
    }

    @Override
    public String getSoftwareVersion() {
        return (String) invokeRMI("getSoftwareVersion", new Object[]{}, new String[]{});
    }

    @Override
    public long getUsed() {
        return (long) invokeRMI("getUsed", new Object[]{}, new String[]{});
    }

    @Override
    public long getFree() {
        return (long) invokeRMI("getFree", new Object[]{}, new String[]{});
    }

    @Override
    public long getTotal() {
        return (long) invokeRMI("getTotal", new Object[]{}, new String[]{});
    }

    @Override
    public long getProvidedCapacity() {
        return (long) invokeRMI("getProvidedCapacity", new Object[]{}, new String[]{});
    }

    @Override
    public String getSafemode() {
        return (String) invokeRMI("getSafemode", new Object[]{}, new String[]{});
    }

    @Override
    public boolean isUpgradeFinalized() {
        return (boolean) invokeRMI("isUpgradeFinalized", new Object[]{}, new String[]{});
    }

    @Override
    public RollingUpgradeInfo.Bean getRollingUpgradeStatus() {
        return (RollingUpgradeInfo.Bean) invokeRMI("getRollingUpgradeStatus", new Object[]{}, new String[]{});
    }

    @Override
    public long getNonDfsUsedSpace() {
        return (long) invokeRMI("getNonDfsUsedSpace", new Object[]{}, new String[]{});
    }

    @Override
    public float getPercentUsed() {
        return (float) invokeRMI("getPercentUsed", new Object[]{}, new String[]{});
    }

    @Override
    public float getPercentRemaining() {
        return (float) invokeRMI("getPercentRemaining", new Object[]{}, new String[]{});
    }

    @Override
    public long getCacheUsed() {
        return (long) invokeRMI("getCacheUsed", new Object[]{}, new String[]{});
    }

    @Override
    public long getCacheCapacity() {
        return (long) invokeRMI("getCacheCapacity", new Object[]{}, new String[]{});
    }

    @Override
    public long getBlockPoolUsedSpace() {
        return (long) invokeRMI("getBlockPoolUsedSpace", new Object[]{}, new String[]{});
    }

    @Override
    public float getPercentBlockPoolUsed() {
        return (float) invokeRMI("getPercentBlockPoolUsed", new Object[]{}, new String[]{});
    }

    @Override
    public long getTotalBlocks() {
        return (long) invokeRMI("getTotalBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getNumberOfMissingBlocks() {
        return (long) invokeRMI("getNumberOfMissingBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getNumberOfMissingBlocksWithReplicationFactorOne() {
        return (long) invokeRMI("getNumberOfMissingBlocksWithReplicationFactorOne", new Object[]{}, new String[]{});
    }

    @Override
    public long getHighestPriorityLowRedundancyReplicatedBlocks() {
        return (long) invokeRMI("getHighestPriorityLowRedundancyReplicatedBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getHighestPriorityLowRedundancyECBlocks() {
        return (long) invokeRMI("getHighestPriorityLowRedundancyECBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getNumberOfSnapshottableDirs() {
        return (long) invokeRMI("getNumberOfSnapshottableDirs", new Object[]{}, new String[]{});
    }

    @Override
    public int getThreads() {
        return (int) invokeRMI("getThreads", new Object[]{}, new String[]{});
    }

    @Override
    public String getLiveNodes() {
        return (String) invokeRMI("getLiveNodes", new Object[]{}, new String[]{});
    }

    @Override
    public String getDeadNodes() {
        return (String) invokeRMI("getDeadNodes", new Object[]{}, new String[]{});
    }

    @Override
    public String getDecomNodes() {
        return (String) invokeRMI("getDecomNodes", new Object[]{}, new String[]{});
    }

    @Override
    public String getEnteringMaintenanceNodes() {
        return (String) invokeRMI("getEnteringMaintenanceNodes", new Object[]{}, new String[]{});
    }

    @Override
    public void fetchProvider_createKey_flush(String keyName, byte[] keyData) throws NoSuchAlgorithmException, IOException {
        invokeRMI("fetchProvider_createKey_flush", new Object[]{keyName, keyData}, new String[]{String.class.getName(), byte[].class.getName()});
    }

    @Override
    public String fetchClusterId() {
        return (String) invokeRMI("fetchClusterId", new Object[]{}, new String[]{});
    }

    @Override
    public String getClusterId() {
        return (String) invokeRMI("getClusterId", new Object[]{}, new String[]{});
    }

    @Override
    public String getBlockPoolId() {
        return (String) invokeRMI("getBlockPoolId", new Object[]{}, new String[]{});
    }

    @Override
    public String getNameDirStatuses() {
        return (String) invokeRMI("getNameDirStatuses", new Object[]{}, new String[]{});
    }

    @Override
    public String getNodeUsage() {
        return (String) invokeRMI("getNodeUsage", new Object[]{}, new String[]{});
    }

    @Override
    public String getNameJournalStatus() {
        return (String) invokeRMI("getNameJournalStatus", new Object[]{}, new String[]{});
    }

    @Override
    public String getJournalTransactionInfo() {
        return (String) invokeRMI("getJournalTransactionInfo", new Object[]{}, new String[]{});
    }

    @Override
    public long getNNStartedTimeInMillis() {
        return (long) invokeRMI("getNNStartedTimeInMillis", new Object[]{}, new String[]{});
    }

    @Override
    public String getCompileInfo() {
        return (String) invokeRMI("getCompileInfo", new Object[]{}, new String[]{});
    }

    @Override
    public String getCorruptFiles() {
        return (String) invokeRMI("getCorruptFiles", new Object[]{}, new String[]{});
    }

    @Override
    public int getCorruptFilesCount() {
        return (int) invokeRMI("getCorruptFilesCount", new Object[]{}, new String[]{});
    }

    @Override
    public int getDistinctVersionCount() {
        return (int) invokeRMI("getDistinctVersionCount", new Object[]{}, new String[]{});
    }

    @Override
    public Map<String, Integer> getDistinctVersions() {
        return (Map<String, Integer>) invokeRMI("getDistinctVersions", new Object[]{}, new String[]{});
    }

    @Override
    public String getNameDirSize() {
        return (String) invokeRMI("getNameDirSize", new Object[]{}, new String[]{});
    }

    @Override
    public String getVerifyECWithTopologyResult() {
        return (String) invokeRMI("getVerifyECWithTopologyResult", new Object[]{}, new String[]{});
    }

    @Override
    public boolean isRunning() {
        return (boolean) invokeRMI("isRunning", new Object[]{}, new String[]{});
    }

    @Override
    public BlockCollection getBlockCollection(long id) {
        return (BlockCollection) invokeRMI("getBlockCollection", new Object[]{id}, new String[]{long.class.getName()});
    }

    @Override
    public FSDirectory getFSDirectory() {
        return (FSDirectory) invokeRMI("getFSDirectory", new Object[]{}, new String[]{});
    }

    @Override
    public void startSecretManagerIfNecessary() {
        invokeRMI("startSecretManagerIfNecessary", new Object[]{}, new String[]{});
    }

    @Override
    public boolean isInSnapshot(long blockCollectionID) {
        return (boolean) invokeRMI("isInSnapshot", new Object[]{blockCollectionID}, new String[]{long.class.getName()});
    }

    @Override
    public CacheManager getCacheManager() {
        return (CacheManager) invokeRMI("getCacheManager", new Object[]{}, new String[]{});
    }

    @Override
    public HAContext getHAContext() {
        return (HAContext) invokeRMI("getHAContext", new Object[]{}, new String[]{});
    }

    @Override
    public boolean inTransitionToActive() {
        return (boolean) invokeRMI("inTransitionToActive", new Object[]{}, new String[]{});
    }

    @Override
    public void removeXattr(long id, String xattrName) throws IOException {
        invokeRMI("removeXattr", new Object[]{id, xattrName}, new String[]{long.class.getName(), String.class.getName()});
    }

    @Override
    public boolean isInSafeMode() {
        return (boolean) invokeRMI("isInSafeMode", new Object[]{}, new String[]{});
    }

    @Override
    public boolean isInStartupSafeMode() {
        return (boolean) invokeRMI("isInStartupSafeMode", new Object[]{}, new String[]{});
    }

    @Override
    public long getLowRedundancyECBlockGroups() {
        return (long) invokeRMI("getLowRedundancyECBlockGroups", new Object[]{}, new String[]{});
    }

    @Override
    public long getCorruptECBlockGroups() {
        return (long) invokeRMI("getCorruptECBlockGroups", new Object[]{}, new String[]{});
    }

    @Override
    public long getMissingECBlockGroups() {
        return (long) invokeRMI("getMissingECBlockGroups", new Object[]{}, new String[]{});
    }

    @Override
    public long getBytesInFutureECBlockGroups() {
        return (long) invokeRMI("getBytesInFutureECBlockGroups", new Object[]{}, new String[]{});
    }

    @Override
    public long getPendingDeletionECBlocks() {
        return (long) invokeRMI("getPendingDeletionECBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getTotalECBlockGroups() {
        return (long) invokeRMI("getTotalECBlockGroups", new Object[]{}, new String[]{});
    }

    @Override
    public String getEnabledEcPolicies() {
        return (String) invokeRMI("getEnabledEcPolicies", new Object[]{}, new String[]{});
    }

    @Override
    public String getFSState() {
        return (String) invokeRMI("getFSState", new Object[]{}, new String[]{});
    }

    @Override
    public long getBlocksTotal() {
        return (long) invokeRMI("getBlocksTotal", new Object[]{}, new String[]{});
    }

    @Override
    public void provider_deleteKey(String key) throws IOException {
        invokeRMI("provider_deleteKey", new Object[]{key}, new String[]{String.class.getName()});
    }

    @Override
    public void blockManagerClear() {
        invokeRMI("blockManagerClear", new Object[]{}, new String[]{});
    }

    @Override
    public long blockGrpIdGeneratorGetCurrentValue() {
        return (long) invokeRMI("blockGrpIdGeneratorGetCurrentValue", new Object[]{}, new String[]{});
    }

    @Override
    public void blockGrpIdGeneratorSetCurrentValue(long newValue) {
        invokeRMI("blockGrpIdGeneratorSetCurrentValue", new Object[]{newValue}, new String[]{long.class.getName()});
    }

    @Override
    public void blockGrpIdGeneratorSkipTo(long newValue) {
        invokeRMI("blockGrpIdGeneratorSkipTo", new Object[]{newValue}, new String[]{long.class.getName()});
    }

    @Override
    public int getSnapshotManagerGetNumSnapshottableDirs() {
        return (int) invokeRMI("getSnapshotManagerGetNumSnapshottableDirs", new Object[]{}, new String[]{});
    }

    @Override
    public int getSnapshotManagerGetNumSnapshots() {
        return (int) invokeRMI("getSnapshotManagerGetNumSnapshots", new Object[]{}, new String[]{});
    }

    @Override
    public void getSnapshotManagerAndSetAllowNestedSnapshots(boolean allowNestedSnapshots) {
        invokeRMI("getSnapshotManagerAndSetAllowNestedSnapshots", new Object[]{allowNestedSnapshots}, new String[]{boolean.class.getName()});
    }

    @Override
    public long getTotalInodes() {
        return (long) invokeRMI("getTotalInodes", new Object[]{}, new String[]{});
    }

    @Override
    public long getCapacityTotal() {
        return (long) invokeRMI("getCapacityTotal", new Object[]{}, new String[]{});
    }

    @Override
    public long getCapacityRemaining() {
        return (long) invokeRMI("getCapacityRemaining", new Object[]{}, new String[]{});
    }

    @Override
    public long getCapacityUsed() {
        return (long) invokeRMI("getCapacityUsed", new Object[]{}, new String[]{});
    }

    @Override
    public long getProvidedCapacityTotal() {
        return (long) invokeRMI("getProvidedCapacityTotal", new Object[]{}, new String[]{});
    }

    @Override
    public long getFilesTotal() {
        return (long) invokeRMI("getFilesTotal", new Object[]{}, new String[]{});
    }

    @Override
    public long getPendingReplicationBlocks() {
        return (long) invokeRMI("getPendingReplicationBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getPendingReconstructionBlocks() {
        return (long) invokeRMI("getPendingReconstructionBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getUnderReplicatedBlocks() {
        return (long) invokeRMI("getUnderReplicatedBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getLowRedundancyBlocks() {
        return (long) invokeRMI("getLowRedundancyBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getScheduledReplicationBlocks() {
        return (long) invokeRMI("getScheduledReplicationBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public int getTotalLoad() {
        return (int) invokeRMI("getTotalLoad", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumLiveDataNodes() {
        return (int) invokeRMI("getNumLiveDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumDeadDataNodes() {
        return (int) invokeRMI("getNumDeadDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumStaleDataNodes() {
        return (int) invokeRMI("getNumStaleDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumDecomLiveDataNodes() {
        return (int) invokeRMI("getNumDecomLiveDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumDecomDeadDataNodes() {
        return (int) invokeRMI("getNumDecomDeadDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumInServiceLiveDataNodes() {
        return (int) invokeRMI("getNumInServiceLiveDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public int getVolumeFailuresTotal() {
        return (int) invokeRMI("getVolumeFailuresTotal", new Object[]{}, new String[]{});
    }

    @Override
    public long getEstimatedCapacityLostTotal() {
        return (long) invokeRMI("getEstimatedCapacityLostTotal", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumDecommissioningDataNodes() {
        return (int) invokeRMI("getNumDecommissioningDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public String getSnapshotStats() {
        return (String) invokeRMI("getSnapshotStats", new Object[]{}, new String[]{});
    }

    @Override
    public long getMaxObjects() {
        return (long) invokeRMI("getMaxObjects", new Object[]{}, new String[]{});
    }

    @Override
    public long getPendingDeletionBlocks() {
        return (long) invokeRMI("getPendingDeletionBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getBlockDeletionStartTime() {
        return (long) invokeRMI("getBlockDeletionStartTime", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumStaleStorages() {
        return (int) invokeRMI("getNumStaleStorages", new Object[]{}, new String[]{});
    }

    @Override
    public String getTopUserOpCounts() {
        return (String) invokeRMI("getTopUserOpCounts", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumEncryptionZones() {
        return (int) invokeRMI("getNumEncryptionZones", new Object[]{}, new String[]{});
    }

    @Override
    public int getFsLockQueueLength() {
        return (int) invokeRMI("getFsLockQueueLength", new Object[]{}, new String[]{});
    }

    @Override
    public long getTotalSyncCount() {
        return (long) invokeRMI("getTotalSyncCount", new Object[]{}, new String[]{});
    }

    @Override
    public String getTotalSyncTimes() {
        return (String) invokeRMI("getTotalSyncTimes", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumInMaintenanceLiveDataNodes() {
        return (int) invokeRMI("getNumInMaintenanceLiveDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumInMaintenanceDeadDataNodes() {
        return (int) invokeRMI("getNumInMaintenanceDeadDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public int getNumEnteringMaintenanceDataNodes() {
        return (int) invokeRMI("getNumEnteringMaintenanceDataNodes", new Object[]{}, new String[]{});
    }

    @Override
    public long getCurrentTokensCount() {
        return (long) invokeRMI("getCurrentTokensCount", new Object[]{}, new String[]{});
    }

    @Override
    public long getLowRedundancyReplicatedBlocks() {
        return (long) invokeRMI("getLowRedundancyReplicatedBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getCorruptReplicatedBlocks() {
        return (long) invokeRMI("getCorruptReplicatedBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getMissingReplicatedBlocks() {
        return (long) invokeRMI("getMissingReplicatedBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getMissingReplicationOneBlocks() {
        return (long) invokeRMI("getMissingReplicationOneBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getBytesInFutureReplicatedBlocks() {
        return (long) invokeRMI("getBytesInFutureReplicatedBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getPendingDeletionReplicatedBlocks() {
        return (long) invokeRMI("getPendingDeletionReplicatedBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public long getTotalReplicatedBlocks() {
        return (long) invokeRMI("getTotalReplicatedBlocks", new Object[]{}, new String[]{});
    }

    @Override
    public void readLock() {
        invokeRMI("readLock", new Object[]{}, new String[]{});
    }

    @Override
    public void readLockInterruptibly() throws InterruptedException {
        invokeRMI("readLockInterruptibly", new Object[]{}, new String[]{});
    }

    @Override
    public void readUnlock() {
        invokeRMI("readUnlock", new Object[]{}, new String[]{});
    }

    @Override
    public boolean hasReadLock() {
        return (boolean) invokeRMI("hasReadLock", new Object[]{}, new String[]{});
    }

    @Override
    public void writeLock() {
        invokeRMI("writeLock", new Object[]{}, new String[]{});
    }

    @Override
    public void writeLockInterruptibly() throws InterruptedException {
        invokeRMI("writeLockInterruptibly", new Object[]{}, new String[]{});
    }

    @Override
    public void writeUnlock() {
        invokeRMI("writeUnlock", new Object[]{}, new String[]{});
    }

    @Override
    public boolean hasWriteLock() {
        return (boolean) invokeRMI("hasWriteLock", new Object[]{}, new String[]{});
    }
}
