package org.apache.hadoop.hdfs.protocol;

public interface LocatedBlockJVMInterface {
    DatanodeInfoJVMInterface[] getLocations();
}
