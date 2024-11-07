package org.apache.hadoop.hdfs.remoteProxies;

public interface ShortCircuitShmInterface {
    SlotInterface allocAndRegisterSlot(ExtendedBlockIdInterface arg0);
    SlotIteratorInterface slotIterator();
    boolean isEmpty();
    UnsafeInterface safetyDance();
    boolean isFull();
    int getUsableLength(java.io.FileInputStream arg0) throws java.io.IOException;
    long calculateSlotAddress(int arg0);
    ShmIdInterface getShmId();
    void unregisterSlot(int arg0);
    java.lang.String toString();
    SlotInterface registerSlot(int arg0, ExtendedBlockIdInterface arg1) throws org.apache.hadoop.fs.InvalidRequestException;
    SlotInterface getSlot(int arg0) throws org.apache.hadoop.fs.InvalidRequestException;
    void free();
}