package org.apache.hadoop.hdfs.protocol;

import java.net.URI;

public interface BlockJVMInterface {
    long getBlockId();
    long getNumBytes();
    long getGenerationStamp();
}
