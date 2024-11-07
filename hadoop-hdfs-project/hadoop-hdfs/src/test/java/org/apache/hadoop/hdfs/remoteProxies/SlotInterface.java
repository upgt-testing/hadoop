package org.apache.hadoop.hdfs.remoteProxies;

public interface SlotInterface {
    boolean isAnchorable();
    boolean isSet(long arg0);
    java.lang.String toString();
    void makeUnanchorable();
    void clearFlag(long arg0);
    boolean isValid();
    void makeValid();
    void removeAnchor();
    void clear();
    ShortCircuitShmInterface getShm();
    boolean isAnchored();
    void setFlag(long arg0);
    void makeInvalid();
    boolean addAnchor();
    ExtendedBlockIdInterface getBlockId();
    int getSlotIdx();
    SlotIdInterface getSlotId();
    void makeAnchorable();
}