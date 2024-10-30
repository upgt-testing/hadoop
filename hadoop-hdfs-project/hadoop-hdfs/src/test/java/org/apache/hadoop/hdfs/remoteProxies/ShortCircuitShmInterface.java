package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.ExtendedBlockId;
import org.apache.hadoop.hdfs.shortcircuit.ShortCircuitShm;

public interface ShortCircuitShmInterface {

    ShortCircuitShm.ShmId getShmId();

    boolean isEmpty();

    boolean isFull();

    ShortCircuitShm.Slot allocAndRegisterSlot(ExtendedBlockIdInterface blockId);

    ShortCircuitShm.Slot getSlot(int slotIdx);

    ShortCircuitShm.Slot registerSlot(int slotIdx, ExtendedBlockId blockId);

    void unregisterSlot(int slotIdx);

    ShortCircuitShm.SlotIterator slotIterator();

    void free();

    String toString();
}
