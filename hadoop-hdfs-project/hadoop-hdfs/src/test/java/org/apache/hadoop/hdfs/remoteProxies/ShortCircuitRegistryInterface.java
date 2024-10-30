package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.ExtendedBlockId;
import org.apache.hadoop.hdfs.server.datanode.ShortCircuitRegistry;
import org.apache.hadoop.hdfs.shortcircuit.ShortCircuitShm;

public interface ShortCircuitRegistryInterface {

    void removeShm(ShortCircuitShmInterface shm);

    void processBlockMlockEvent(ExtendedBlockId blockId);

    boolean processBlockMunlockRequest(ExtendedBlockId blockId);

    void processBlockInvalidation(ExtendedBlockId blockId);

    String getClientNames(ExtendedBlockId blockId);

    ShortCircuitRegistry.NewShmInfo createNewMemorySegment(String clientName, DomainSocketInterface sock);

    void registerSlot(ExtendedBlockId blockId, ShortCircuitShm.SlotId slotId, boolean isCached);

    void unregisterSlot(ShortCircuitShm.SlotId slotId);

    void shutdown();

    boolean visit(ShortCircuitRegistry.Visitor visitor);

    int getShmNum();
}
