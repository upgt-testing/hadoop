package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FsStatusJVMInterface extends WritableJVMInterface {

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    long getCapacity();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    long getUsed();

    long getRemaining();
}
