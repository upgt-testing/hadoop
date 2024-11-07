package org.apache.hadoop.hdfs.remoteProxies;

public interface ShortCircuitRegistryInterface {
    NewShmInfoInterface createNewMemorySegment(java.lang.String arg0, DomainSocketInterface arg1) throws java.io.IOException;
    void unregisterSlot(SlotIdInterface arg0) throws org.apache.hadoop.fs.InvalidRequestException;
    void processBlockInvalidation(ExtendedBlockIdInterface arg0);
    int getShmNum();
    boolean visit(org.apache.hadoop.hdfs.server.datanode.ShortCircuitRegistry.Visitor arg0);
    void registerSlot(ExtendedBlockIdInterface arg0, SlotIdInterface arg1, boolean arg2) throws org.apache.hadoop.fs.InvalidRequestException;
    void removeShm(ShortCircuitShmInterface arg0);
    void shutdown();
    void processBlockMlockEvent(ExtendedBlockIdInterface arg0);
    java.lang.String getClientNames(ExtendedBlockIdInterface arg0);
    boolean processBlockMunlockRequest(ExtendedBlockIdInterface arg0);
}